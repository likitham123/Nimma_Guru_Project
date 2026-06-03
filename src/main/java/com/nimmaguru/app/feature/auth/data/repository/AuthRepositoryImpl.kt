package com.nimmaguru.app.feature.auth.data.repository

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.nimmaguru.app.BuildConfig
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.PhoneAuthEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Auth repository implementation.
 *
 * R-FB-05: Firebase SDK calls ONLY here.
 * R-ARCH-03: Repository = Single Source of Truth.
 *
 * Phone-auth is implemented via a [MutableSharedFlow] that bridges Firebase's
 * callback-based [PhoneAuthProvider.OnVerificationStateChangedCallbacks] into
 * our domain [PhoneAuthEvent] flow. Auto-verification (where the carrier
 * silently signs the user in) is handled here by performing the
 * `signInWithCredential` synchronously and emitting [PhoneAuthEvent.AutoVerified]
 * with the resulting UID.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    /**
     * Replay = 1 so that an OtpVerifyScreen freshly inflated AFTER a successful
     * `CodeSent` (PhoneEntry → OtpVerify navigation) still sees the latest
     * event. Otherwise the late subscriber would miss it and the second
     * AuthViewModel instance would be wedged in `Idle`.
     */
    private val _phoneAuthEvents = MutableSharedFlow<PhoneAuthEvent>(
        replay = 1,
        extraBufferCapacity = 4,
    )

    /** Source-of-truth verificationId, shared across AuthViewModel instances. */
    @Volatile
    private var _currentVerificationId: String? = null

    /** Long-lived scope for callback work that must outlive the calling VM. */
    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Migrate users away from any anonymous account left behind by the
        // previous simulated-auth build so they're forced through the real
        // OTP flow on next launch. Safe no-op if currentUser is real or null.
        firebaseAuth.currentUser
            ?.takeIf { it.isAnonymous }
            ?.let { firebaseAuth.signOut() }
    }

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override val currentUserPhone: String?
        get() = firebaseAuth.currentUser?.phoneNumber

    override val currentVerificationId: String?
        get() = _currentVerificationId

    /**
     * "Logged in" = a non-anonymous Firebase user is present. Anonymous users
     * are treated as not-logged-in so any leftover anonymous account from the
     * previous simulated-auth build is forced through the real OTP flow.
     */
    override val isLoggedIn: Boolean
        get() = firebaseAuth.currentUser?.let { !it.isAnonymous } ?: false

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        fun loggedIn(u: com.google.firebase.auth.FirebaseUser?): Boolean =
            u != null && !u.isAnonymous

        // Push initial value so collectors don't wait for the next state change.
        trySend(loggedIn(firebaseAuth.currentUser))
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(loggedIn(auth.currentUser))
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun phoneAuthEvents(): Flow<PhoneAuthEvent> = _phoneAuthEvents.asSharedFlow()

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    override fun startPhoneVerification(phoneNumber: String, activity: Activity) {
        // Reset state for the new verification attempt.
        _currentVerificationId = null
        _phoneAuthEvents.resetReplayCache()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "startPhoneVerification: $phoneNumber")
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onVerificationCompleted (auto-verify)")
                authScope.launch {
                    try {
                        val authResult = firebaseAuth.signInWithCredential(credential).await()
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            _phoneAuthEvents.emit(PhoneAuthEvent.AutoVerified(uid))
                        } else {
                            _phoneAuthEvents.emit(PhoneAuthEvent.Failed("Auto-verify produced no user"))
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) Log.e(TAG, "Auto-verify sign-in failed", e)
                        _phoneAuthEvents.emit(
                            PhoneAuthEvent.Failed(e.message ?: "Auto-verify failed")
                        )
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // Surface Firebase's actual reason in Logcat so the developer
                // can diagnose: invalid format, BILLING_NOT_ENABLED, App Check
                // token rejected, SHA-1 missing, etc.
                Log.e(TAG, "Phone verification failed: ${e.javaClass.simpleName}: ${e.message}", e)
                val userFacing = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number format."
                    else -> e.message ?: "Verification failed"
                }
                authScope.launch {
                    _phoneAuthEvents.emit(PhoneAuthEvent.Failed(userFacing))
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                if (BuildConfig.DEBUG) Log.d(TAG, "onCodeSent: vid=$verificationId")
                _currentVerificationId = verificationId
                authScope.launch {
                    _phoneAuthEvents.emit(PhoneAuthEvent.CodeSent(verificationId))
                }
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(Constants.OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override suspend fun verifyOtp(verificationId: String, otp: String): Result<String> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid
            if (uid != null) {
                _currentVerificationId = null // consumed
                Result.success(uid)
            } else {
                Result.failure(Exception("Authentication succeeded but user ID is null"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "verifyOtp failed: ${e.javaClass.simpleName}: ${e.message}", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        _currentVerificationId = null
        firebaseAuth.signOut()
    }

    private companion object {
        const val TAG = "NimmaGuruAuth"
    }
}
