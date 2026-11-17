package com.quickgig.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel, onSignedIn: () -> Unit) {
    val state = viewModel.uiState

    // Navigate from an effect rather than in the middle of composition, so it
    // fires exactly once when sign-in succeeds.
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedIn()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportedLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { viewModel.onLanguageSelected(lang) },
                        label = { Text(lang.code.uppercase()) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            if (state.mode == LoginUiState.Mode.REGISTER) {
                OutlinedTextField(
                    value = state.name, onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.login_full_name)) }, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = state.email, onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password, onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.login_password)) }, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            val errorText = state.errorRes?.let { stringResource(it) } ?: state.error
            errorText?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            Button(onClick = viewModel::submit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(if (state.mode == LoginUiState.Mode.LOGIN) R.string.login_log_in else R.string.login_create_account))
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.continueWithGoogle(name = "Google Demo User", email = "google-demo@example.com") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.login_continue_google))
            }
            Spacer(Modifier.height(16.dp))

            OutlinedButton(onClick = viewModel::toggleMode, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.mode == LoginUiState.Mode.LOGIN) R.string.login_new_here else R.string.login_have_account))
            }
        }
    }
}
