package com.quickgig.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.quickgig.app.R
import com.quickgig.app.data.model.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onLoggedOut: () -> Unit) {
    val state = viewModel.uiState

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    var phone by remember { mutableStateOf(state.phone.orEmpty()) }
    var campusArea by remember { mutableStateOf(state.campusArea.orEmpty()) }
    var photoUrl by remember { mutableStateOf(state.photoUrl.orEmpty()) }

    LaunchedEffect(state) {
        phone = state.phone.orEmpty()
        campusArea = state.campusArea.orEmpty()
        photoUrl = state.photoUrl.orEmpty()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {

            // Profile photo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                if (photoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberImagePainter(photoUrl),
                        contentDescription = stringResource(R.string.settings_profile_photo),
                        modifier = Modifier.size(72.dp),
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = stringResource(R.string.settings_profile_photo),
                        modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text = state.name.takeIf { it.isNotEmpty() } ?: stringResource(R.string.settings_profile_name_placeholder),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                SupportedLanguage.values().forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { viewModel.onLanguageSelected(lang) },
                        label = { Text(lang.displayName) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Phone
            TextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.settings_phone)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // Campus area
            TextField(
                value = campusArea,
                onValueChange = { campusArea = it },
                label = { Text(stringResource(R.string.settings_campus_area)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // Photo URL
            TextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                label = { Text(stringResource(R.string.settings_photo_url)) },
                modifier = Modifier.fillMaxWidth(),
            )
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

            if (state.settingsSaved) {
                Text(stringResource(R.string.settings_saved), color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    viewModel.onSaveSettings(
                        name = state.name.takeIf { it.isNotBlank() },
                        phone = phone.takeIf { it.isNotBlank() },
                        campusArea = campusArea.takeIf { it.isNotBlank() },
                        photoUrl = photoUrl.takeIf { it.isNotBlank() },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_save_changes))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_logout))
            }
        }
    }
}
