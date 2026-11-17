package com.quickgig.app.ui.postgig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.GigCategory

private fun categoryLabelRes(category: GigCategory): Int = when (category) {
    GigCategory.ALL -> R.string.category_all
    GigCategory.ERRANDS -> R.string.category_errands
    GigCategory.TUTORING -> R.string.category_tutoring
    GigCategory.CREATIVE -> R.string.category_creative
    GigCategory.LABOUR -> R.string.category_labour
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostGigScreen(viewModel: PostGigViewModel, onDone: () -> Unit) {
    val state = viewModel.uiState

    LaunchedEffect(state.result) {
        if (state.result != null) onDone()
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.post_gig_title)) }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.title, onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.post_gig_title_hint)) }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            LazyRow {
                items(GigCategory.values().toList().filter { it != GigCategory.ALL }) { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(stringResource(categoryLabelRes(category))) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.post_gig_description)) }, modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.priceCredits, onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.post_gig_credits)) }, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.post_gig_offline_note),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(16.dp))

            state.errorRes?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(onClick = viewModel::submit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.isSubmitting) R.string.post_gig_posting else R.string.post_gig_button))
            }
        }
    }
}
