package com.nimmaguru.app.feature.auth.presentation

import android.app.Activity
import androidx.annotation.StringRes

/**
 * UI state for the Auth flow.
 * R-KT-06: Sealed hierarchy for UI state — no boolean flags.
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object OtpSent : AuthUiState
    data object Authenticated : AuthUiState
    data class Error(@StringRes val errorRes: Int) : AuthUiState
}

/**
 * One-time events from Auth flow.
 * R-ARCH-02: Events via SharedFlow for navigation/toasts.
 */
sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
    data object NavigateToBasicOnboarding : AuthEvent
    data object NavigateToOtp : AuthEvent
    data class ShowSnackbar(@StringRes val messageRes: Int) : AuthEvent
}

/**
 * User actions on the Auth screen.
 *
 * [SendOtp] / [ResendOtp] carry an [Activity] reference because Firebase
 * Phone Auth requires it for reCAPTCHA fallback. This is the one
 * approved leak of an Android type into action types.
 */
sealed interface AuthAction {
    data class PhoneNumberChanged(val phone: String) : AuthAction
    data class OtpChanged(val otp: String) : AuthAction
    data class SendOtp(val activity: Activity) : AuthAction
    data object VerifyOtp : AuthAction
    data class ResendOtp(val activity: Activity) : AuthAction
    data object ContinueAsGuest : AuthAction
}
