package com.quickgig.app.ui.gigdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.Gig
import com.quickgig.app.ui.theme.QuickGigPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GigDetailScreen(
    viewModel: GigDetailViewModel,
    onBack: () -> Unit,
    onNavigateChat: (Long) -> Unit,
) {
    val state = viewModel.uiState
    val gig = state.gig ?: return

    LaunchedEffect(state.error) {
        if (state.error != null) {
            // In a production app we'd surface this in a Snackbar.
        }
    }

    // Local rating stars while the user picks before submitting.
    var localStars by remember { mutableStateOf(state.ratingStars) }
    var localOnTime by remember { mutableStateOf(state.onTime) }

    LaunchedEffect(state) {
        localStars = state.ratingStars
        localOnTime = state.onTime
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gig_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ---- Gig header card ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(gig.title, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Work,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = gig.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        if (gig.priceCredits > 0) {
                            Text(
                                text = stringResource(R.string.gig_meta_credits, gig.priceCredits),
                                style = MaterialTheme.typography.titleMedium,
                                color = QuickGigPurple,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.gig_meta_unpaid),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Divider()
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gig_detail_posted_by, gig.posterName ?: "—"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (gig.neededBy != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.gig_detail_needed_by, gig.neededBy),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gig_detail_map),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // ---- Status-dependent actions ----
            when (gig.status) {
                "open" -> {
                    val alreadyApplied = gig.myApplication != null
                    Button(
                        onClick = {
                            if (alreadyApplied) return@Button
                            viewModel.onApply()
                        },
                        enabled = !alreadyApplied && gig.postedBy != viewModel.currentUserId,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (alreadyApplied) stringResource(R.string.gig_detail_applied) else stringResource(R.string.gig_detail_apply))
                    }
                    if (gig.postedBy == viewModel.currentUserId) {
                        TextButton(
                            onClick = { viewModel.onCancel() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.gig_detail_cancel))
                        }
                    }
                }
                "assigned" -> {
                    if (gig.assignedTo == viewModel.currentUserId) {
                        Button(
                            onClick = { viewModel.onWithdraw() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.gig_detail_withdraw))
                        }
                    } else if (gig.postedBy == viewModel.currentUserId) {
                        // Poster can complete the gig (rate the worker)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(R.string.gig_detail_rating_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { viewModel.onRatingChanged(star); localStars = star },
                                    ) {
                                        Icon(
                                            imageVector = if (star <= localStars) Icons.Filled.Star else Icons.Filled.Circle,
                                            contentDescription = null,
                                            tint = if (star <= localStars) QuickGigPurple else MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.gig_detail_on_time),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Spacer(Modifier.width(8.dp))
                                Switch(
                                    checked = localOnTime,
                                    onCheckedChange = { viewModel.onOnTimeChanged(it); localOnTime = it },
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (localStars == 0) return@Button
                                    viewModel.onComplete(localStars, localOnTime)
                                },
                                enabled = localStars > 0,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.gig_detail_complete))
                            }
                        }
                    }
                }
                "completed" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.gig_detail_status_completed),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                "cancelled" -> {
                    if (gig.postedBy == viewModel.currentUserId) {
                        TextButton(
                            onClick = { viewModel.onReopen() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.gig_detail_open_again))
                        }
                    }
                }
            }

            // ---- Applicants (poster only, when open) ----
            if (gig.status == "open" && gig.postedBy == viewModel.currentUserId) {
                Text(stringResource(R.string.gig_detail_applicants), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (gig.applicantCount == 0) {
                    Text(stringResource(R.string.gig_detail_no_applicants), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(gig.applicantCount) { index ->
                            // Placeholder rows — the real applicant list comes from the API's /applicants endpoint.
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Applicant $index", style = MaterialTheme.typography.bodyMedium)
                                    Text("Hustle Score —", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            // ---- Map (Gig location) ----
            if (gig.location.lat != null && gig.location.lng != null) {
                GigLocationMap(
                    lat = gig.location.lat!!,
                    lng = gig.location.lng!!,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Simple placeholder map card that renders a static map thumbnail.
 * The real Google Maps Compose integration requires the API key in the manifest. */
@Composable
private fun GigLocationMap(lat: Double, lng: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.gig_detail_map), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Lat: $lat, Lng: $lng", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Tap to open on map", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
