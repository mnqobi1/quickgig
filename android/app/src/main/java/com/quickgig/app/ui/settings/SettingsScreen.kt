package com.quickgig.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val state = viewModel.uiState

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                SupportedLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { viewModel.onLanguageSelected(lang) },
                        label = { Text(lang.displayName) }, // each language written in itself, so it's findable from any UI language
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_notifications_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_notifications_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::onNotificationsToggled)
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_location_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.settings_location_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = state.shareExactLocation, onCheckedChange = viewModel::onShareExactLocationToggled)
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.settings_payment_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_payment_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(24.dp))

            if (state.languageSaved) {
                Text(stringResource(R.string.settings_language_updated), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
            }

            Button(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_logout))
            }
        }
    }
}
