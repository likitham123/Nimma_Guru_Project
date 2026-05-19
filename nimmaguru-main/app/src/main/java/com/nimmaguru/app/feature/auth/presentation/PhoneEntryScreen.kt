package com.nimmaguru.app.feature.auth.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nimmaguru.app.core.common.findActivity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nimmaguru.app.R
import com.nimmaguru.app.core.ui.theme.NimmaGuruTheme

/**
 * Phone Entry Screen — first step of Auth flow.
 * idea-screens.md Screen 7.
 *
 * R-COMP-01: Stateless content composable.
 * R-COMP-04: 18sp body, 56 dp button height.
 * P-COMP-04: collectAsStateWithLifecycle.
 *
 * The screen extracts the host Activity via LocalContext and passes it
 * to the ViewModel because Firebase Phone Auth requires it for
 * reCAPTCHA fallback (see P0.2).
 */
@Composable
fun PhoneEntryScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToOtp: (String) -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToBasicOnboarding: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val activity: Activity? = LocalContext.current.findActivity()

    // Handle navigation events. Note: auto-verification (instant carrier
    // verification) emits NavigateToHome / NavigateToBasicOnboarding from
    // THIS screen — so we must handle both. The phoneNumber displayed on
    // OtpVerifyScreen is read from the latest local Compose state.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToOtp -> onNavigateToOtp("+91${viewModel.phoneNumber.value}")
                is AuthEvent.NavigateToBasicOnboarding -> onNavigateToBasicOnboarding()
                is AuthEvent.ShowSnackbar -> { /* host has no snackbar here */ }
            }
        }
    }

    PhoneEntryContent(
        modifier = modifier,
        phoneNumber = phoneNumber,
        uiState = uiState,
        onSendOtp = { activity?.let { viewModel.onAction(AuthAction.SendOtp(it)) } },
        onPhoneNumberChanged = { viewModel.onAction(AuthAction.PhoneNumberChanged(it)) },
        onContinueAsGuest = { viewModel.onAction(AuthAction.ContinueAsGuest) },
    )
}

@Composable
fun PhoneEntryContent(
    modifier: Modifier = Modifier,
    phoneNumber: String = "",
    uiState: AuthUiState = AuthUiState.Idle,
    onSendOtp: () -> Unit = {},
    onPhoneNumberChanged: (String) -> Unit = {},
    onContinueAsGuest: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🙏",
            style = MaterialTheme.typography.displayLarge,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.phone_prefix),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 8.dp),
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { value ->
                    if (value.length <= 10 && value.all { it.isDigit() }) {
                        onPhoneNumberChanged(value)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = MaterialTheme.shapes.medium,
            )
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(uiState.errorRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSendOtp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState !is AuthUiState.Loading && phoneNumber.length == 10,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.send_otp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.or_divider),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onContinueAsGuest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = stringResource(R.string.continue_as_guest),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_PhoneEntryScreen_Idle() {
    NimmaGuruTheme {
        PhoneEntryContent(
            phoneNumber = "",
            uiState = AuthUiState.Idle,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_PhoneEntryScreen_Error() {
    NimmaGuruTheme {
        PhoneEntryContent(
            phoneNumber = "987",
            uiState = AuthUiState.Error(R.string.error_invalid_phone),
        )
    }
}
