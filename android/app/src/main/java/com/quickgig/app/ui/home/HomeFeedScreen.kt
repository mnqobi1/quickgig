package com.quickgig.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.GigCategory
import com.quickgig.app.ui.components.GigCard
import com.quickgig.app.ui.components.QuickGigBottomBar

private fun categoryLabelRes(category: GigCategory): Int = when (category) {
    GigCategory.ALL -> R.string.category_all
    GigCategory.ERRANDS -> R.string.category_errands
    GigCategory.TUTORING -> R.string.category_tutoring
    GigCategory.CREATIVE -> R.string.category_creative
    GigCategory.LABOUR -> R.string.category_labour
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun HomeFeedScreen(
    viewModel: HomeViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenGig: (Long) -> Unit,
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        bottomBar = { QuickGigBottomBar(currentRoute, onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (state.pendingDraftCount > 0) {
                Card(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        text = stringResource(R.string.home_offline_banner, state.pendingDraftCount),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            FilterChipsRow(state, viewModel)

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.gigs) { gig ->
                    GigCard(gig = gig, onClick = { onOpenGig(gig.id) })
                }
            }
        }
    }
}

@Composable
private fun FilterChipsRow(state: HomeUiState, viewModel: HomeViewModel) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = false,
                onClick = { viewModel.onRadiusSelected(if (state.radiusKm == 5.0) 15.0 else 5.0) },
                label = { Text(stringResource(R.string.home_radius_km, state.radiusKm.toInt())) },
            )
        }
        items(GigCategory.values().toList()) { category ->
            FilterChip(
                selected = state.category == category,
                onClick = { viewModel.onCategorySelected(category) },
                label = { Text(stringResource(categoryLabelRes(category))) },
            )
        }
    }
}
