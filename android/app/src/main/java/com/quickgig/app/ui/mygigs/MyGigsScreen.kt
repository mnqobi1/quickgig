package com.quickgig.app.ui.mygigs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.ui.components.GigCard
import com.quickgig.app.ui.components.QuickGigBottomBar

private fun tabLabelRes(tab: MyGigsTab): Int = when (tab) {
    MyGigsTab.POSTED -> R.string.tab_posted
    MyGigsTab.APPLIED -> R.string.tab_applied
    MyGigsTab.ACTIVE -> R.string.tab_active
    MyGigsTab.COMPLETED -> R.string.tab_completed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGigsScreen(
    viewModel: MyGigsViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onOpenGig: (Long) -> Unit,
) {
    val state = viewModel.uiState

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.my_gigs_title)) }) },
        bottomBar = { QuickGigBottomBar(currentRoute, onNavigate) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = state.tab.ordinal) {
                MyGigsTab.values().forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(stringResource(tabLabelRes(tab))) },
                    )
                }
            }

            if (state.drafts.isNotEmpty() && state.tab == MyGigsTab.POSTED) {
                Text(
                    stringResource(R.string.my_gigs_drafts_pending, state.drafts.size),
                    modifier = Modifier.padding(12.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.gigs) { gig ->
                    GigCard(gig = gig, onClick = { onOpenGig(gig.id) })
                }
            }
        }
    }
}
