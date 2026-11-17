package com.quickgig.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.ui.components.QuickGigBottomBar

private fun badgeLabelRes(code: String): Int? = when (code) {
    "verified_student" -> R.string.badge_verified_student
    "top_rated" -> R.string.badge_top_rated
    "reliable_runner" -> R.string.badge_reliable_runner
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        bottomBar = { QuickGigBottomBar(currentRoute, onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                state.profile != null -> {
                    val profile = state.profile
                    Text(profile.name, style = MaterialTheme.typography.headlineMedium)
                    if (profile.verificationStatus == "verified") {
                        Text(stringResource(R.string.profile_verified_student), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${profile.hustleScore}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.profile_hustle_score_suffix), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.profile_stats, profile.completedGigs, profile.averageRating.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(stringResource(R.string.profile_badges_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (profile.badges.isEmpty()) {
                        Text(stringResource(R.string.profile_no_badges), color = MaterialTheme.colorScheme.outline)
                    } else {
                        LazyRow {
                            items(profile.badges) { badge ->
                                val labelRes = badgeLabelRes(badge.badgeCode)
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (labelRes != null) stringResource(labelRes) else badge.badgeCode) },
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
