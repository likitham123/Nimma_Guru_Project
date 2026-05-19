package com.nimmaguru.app.feature.auth.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.R
import com.nimmaguru.app.core.common.Validators
import com.nimmaguru.app.feature.auth.domain.repository.AuthRepository
import com.nimmaguru.app.feature.auth.domain.repository.PhoneAuthEvent
import com.nimmaguru.app.feature.auth.domain.repository.UserRepository
import com.nimmaguru.app.feature.auth.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Auth flow (Phone Entry + OTP Verification).
 *
 * Real Firebase Phone Auth — no simulation, no anonymous fallback (P0.2 / C1).
 *
 * Important scoping note: Compose's NavHost gives `PhoneEntryRoute` and
 * `OtpVerifyRoute` separate `NavBackStackEntry` instances, so each
 * `hiltViewModel()` call creates its OWN AuthViewModel. The verificationId
 * from `CodeSent` is therefore NOT carried in this VM's local field across
 * screens — it lives on the singleton [AuthRepository] (`currentVerificationId`)
 * and is read from there in [verifyOtp].
 *
 * The repo's `phoneAuthEvents` is configured with `replay = 1`, so the second
 * VM that subscribes (the OTP screen's) re-receives the latest event and can
 * also drive its own UI state without losing context.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val verifyOtpUseCase: VerifyOtpUseCase,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Buffered + DROP_OLDEST so a fast `emit` from a coroutine body never
     * silently drops a NavigateToOtp / NavigateToHome event, even if the
     * collector hasn't started yet.
     */
    private val _events = MutableSharedFlow<AuthEvent>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otpCode = MutableStateFlow("")
    val otpCode: StateFlow<String> = _otpCode.asStateFlow()

    /**
     * Tracks whether THIS VM instance has already reacted to a CodeSent event,
     * so that a replayed event arriving on the OTP screen's VM doesn't
     * re-emit a NavigateToOtp into a screen that is already there.
     */
    private var codeSentHandled: Boolean = false

    init {
        // Listen for phone-auth events from the repository for the lifetime of this VM.
        viewModelScope.launch {
            authRepository.phoneAuthEvents().collect { event ->
                when (event) {
                    is PhoneAuthEvent.CodeSent -> {
                        // Repo already stored the verificationId; we just react to UI/nav.
                        _uiState.value = AuthUiState.OtpSent
                        if (!codeSentHandled) {
                            codeSentHandled = true
                            _events.emit(AuthEvent.NavigateToOtp)
                        }
                    }
                    is PhoneAuthEvent.AutoVerified -> {
                        _uiState.value = AuthUiState.Authenticated
                        handlePostLoginNavigation(event.uid)
                    }
                    is PhoneAuthEvent.Failed -> {
                        _uiState.value = AuthUiState.Error(R.string.error_otp_send_failed)
                    }
                }
            }
        }
    }

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.PhoneNumberChanged -> _phoneNumber.value = action.phone
            is AuthAction.OtpChanged -> _otpCode.value = action.otp
            is AuthAction.SendOtp -> sendOtp(action.activity)
            is AuthAction.VerifyOtp -> verifyOtp()
            is AuthAction.ResendOtp -> sendOtp(action.activity)
            is AuthAction.ContinueAsGuest -> continueAsGuest()
        }
    }

    private fun sendOtp(activity: Activity) {
        if (!Validators.isValidPhone(_phoneNumber.value)) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_phone)
            return
        }
        // Reset the per-VM CodeSent guard so a re-sent OTP can re-trigger nav.
        codeSentHandled = false
        _uiState.value = AuthUiState.Loading
        // Repo runs the verification asynchronously; results arrive via phoneAuthEvents.
        authRepository.startPhoneVerification(
            phoneNumber = "+91${_phoneNumber.value}",
            activity = activity,
        )
    }

    private fun verifyOtp() {
        val otp = _otpCode.value
        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error(R.string.error_invalid_otp)
            return
        }
        // Read from the repo, NOT a local field — local field is empty on the
        // OTP screen's separate VM instance (Bug A).
        val vId = authRepository.currentVerificationId
        if (vId.isNullOrEmpty()) {
            _uiState.value = AuthUiState.Error(R.string.error_verification_failed)
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            verifyOtpUseCase(vId, otp)
                .onSuccess { uid ->
                    _uiState.value = AuthUiState.Authenticated
                    handlePostLoginNavigation(uid)
                }
                .onFailure {
                    _uiState.value = AuthUiState.Error(R.string.error_verification_failed)
                }
        }
    }

    private fun handlePostLoginNavigation(uid: String) {
        viewModelScope.launch {
            userRepository.getUser(uid)
                .onSuccess { user ->
                    if (user != null) {
                        _events.emit(AuthEvent.NavigateToHome)
                    } else {
                        _events.emit(AuthEvent.NavigateToBasicOnboarding)
                    }
                }
                .onFailure {
                    // If Firestore read fails, send to onboarding as a safe default
                    // (won't double-create because saveUser uses merge).
                    _events.emit(AuthEvent.NavigateToBasicOnboarding)
                }
        }
    }

    private fun continueAsGuest() {
        viewModelScope.launch {
            _events.emit(AuthEvent.NavigateToHome)
        }
    }
}
