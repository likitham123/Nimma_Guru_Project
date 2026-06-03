package com.nimmaguru.app.feature.auth.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.Flow

/**
 * Auth repository interface — defined in domain layer.
 *
 * R-ARCH-03: Repository interface in domain, implementation in data.
 * R-FB-05: Domain MUST NOT reference Firebase types directly. Phone Auth
 * results are surfaced via the [PhoneAuthEvent] sealed class below.
 *
 * `Activity` is the one Android-framework type we accept here because
 * Firebase Phone Auth structurally requires it for reCAPTCHA fallback.
 */
interface AuthRepository {
    val currentUserId: String?
    val currentUserPhone: String?
    val isLoggedIn: Boolean

    /**
     * Latest verificationId received from a `PhoneAuthEvent.CodeSent` emission,
     * or null if no verification is currently in flight. Held on the
     * (singleton) repository so that the OTP screen — which lives in a
     * different NavBackStackEntry and therefore has its own AuthViewModel
     * instance — can still verify the OTP that PhoneEntryScreen requested.
     */
    val currentVerificationId: String?

    fun observeAuthState(): Flow<Boolean>

    /**
     * Stream of phone-auth lifecycle events. The screen / VM observes
     * this to react to code-sent / auto-verified / failed states.
     */
    fun phoneAuthEvents(): Flow<PhoneAuthEvent>

    /**
     * Kicks off the phone-auth flow. Sends an OTP SMS for [phoneNumber]
     * (must include country code, e.g. "+919876543210"). Results are
     * delivered asynchronously via [phoneAuthEvents].
     */
    fun startPhoneVerification(phoneNumber: String, activity: Activity)

    /**
     * Submits the user-typed OTP against the verificationId previously
     * received in [PhoneAuthEvent.CodeSent].
     *
     * @return Result.success with the signed-in user's UID, or failure.
     */
    suspend fun verifyOtp(verificationId: String, otp: String): Result<String>

    suspend fun signOut()
}

/** Domain-level phone auth events. No Firebase types leak here. */
sealed interface PhoneAuthEvent {
    /** OTP SMS dispatched. Caller stores [verificationId] and prompts user to type the code. */
    data class CodeSent(val verificationId: String) : PhoneAuthEvent

    /** Carrier auto-verified the device — user is already signed in with [uid]. */
    data class AutoVerified(val uid: String) : PhoneAuthEvent

    /** Verification failed (network, throttle, invalid number, etc.). */
    data class Failed(val reason: String) : PhoneAuthEvent
}
