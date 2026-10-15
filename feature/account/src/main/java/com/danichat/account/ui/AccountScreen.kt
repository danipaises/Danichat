package com.danichat.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.danichat.account.data.AccountFailure

@Composable
fun AccountScreen(viewModel: AccountViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.account_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.account_optional), style = MaterialTheme.typography.bodyLarge)

        if (!state.configured) {
            Card(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_unconfigured), Modifier.padding(20.dp))
            }
        } else if (state.user == null) {
            LoginForm(state, viewModel)
        } else {
            Text(stringResource(R.string.account_connected), style = MaterialTheme.typography.titleMedium)
            val profile = state.profile
            if (profile != null) {
                var name by remember(profile) { mutableStateOf(profile.displayName) }
                var bio by remember(profile) { mutableStateOf(profile.bio) }
                val nameLength = name.codePointCount(0, name.length)
                val bioLength = bio.codePointCount(0, bio.length)
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_name)) }, singleLine = true,
                    enabled = !state.busy, isError = nameLength > 80,
                    supportingText = { Text(stringResource(R.string.account_count, nameLength, 80)) }
                )
                OutlinedTextField(
                    value = bio, onValueChange = { bio = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.account_bio)) }, minLines = 3, maxLines = 6,
                    enabled = !state.busy, isError = bioLength > 500,
                    supportingText = { Text(stringResource(R.string.account_count, bioLength, 500)) }
                )
                Button(
                    onClick = { viewModel.saveProfile(name, bio) }, modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy && name.isNotBlank() && nameLength <= 80 && bioLength <= 500
                ) { Text(stringResource(R.string.account_save)) }
            } else {
                Text(stringResource(R.string.account_profile_unavailable))
            }
            OutlinedButton(onClick = viewModel::retry, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_reload)) }
            OutlinedButton(onClick = viewModel::signOut, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.account_sign_out)) }
        }

        if (state.busy) CircularProgressIndicator()
        state.failure?.let { Text(stringResource(errorResource(it)), color = MaterialTheme.colorScheme.error) }
        state.notice?.let { Text(stringResource(noticeResource(it))) }
        Text(stringResource(R.string.account_privacy), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LoginForm(state: AccountUiState, viewModel: AccountViewModel) {
    // Credentials are deliberately absent from saved state and ViewModel state.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.clearCredentialsVersion) { email = ""; password = "" }
    OutlinedTextField(
        value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.account_email)) },
        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.busy,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
    )
    OutlinedTextField(
        value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.account_password)) }, singleLine = true, enabled = !state.busy,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        supportingText = { Text(stringResource(R.string.account_password_hint)) }
    )
    Button(onClick = { viewModel.signIn(email, password) }, modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy && email.isNotBlank() && password.isNotEmpty()) {
        Text(stringResource(R.string.account_sign_in))
    }
    OutlinedButton(onClick = { viewModel.signUp(email, password) }, modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy && email.isNotBlank() && password.length >= 12) {
        Text(stringResource(R.string.account_sign_up))
    }
}

private fun errorResource(failure: AccountFailure): Int = when (failure) {
    AccountFailure.NETWORK -> R.string.account_error_network
    AccountFailure.INVALID_CREDENTIALS -> R.string.account_error_credentials
    AccountFailure.EMAIL_NOT_CONFIRMED -> R.string.account_error_confirmation
    AccountFailure.RATE_LIMITED -> R.string.account_error_rate_limit
    AccountFailure.INVALID_INPUT -> R.string.account_error_input
    AccountFailure.SESSION_EXPIRED -> R.string.account_error_session
    AccountFailure.SERVER_NOT_READY -> R.string.account_error_setup
    AccountFailure.SERVICE_UNAVAILABLE -> R.string.account_error_service
    AccountFailure.LOCAL_STORAGE -> R.string.account_error_storage
}

private fun noticeResource(notice: AccountNotice): Int = when (notice) {
    AccountNotice.CONFIRM_EMAIL -> R.string.account_confirm_email
    AccountNotice.PROFILE_SAVED -> R.string.account_saved
    AccountNotice.SIGNED_OUT -> R.string.account_signed_out
    AccountNotice.SIGNED_OUT_LOCALLY -> R.string.account_signed_out_locally
}
