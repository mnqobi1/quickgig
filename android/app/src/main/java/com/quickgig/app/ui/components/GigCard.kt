package com.quickgig.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickgig.app.R
import com.quickgig.app.data.model.Gig

@Composable
fun GigCard(gig: Gig, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Work,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(gig.title, style = MaterialTheme.typography.titleMedium)
                val priceText = if (gig.priceCredits > 0) {
                    stringResource(R.string.gig_meta_credits, gig.priceCredits)
                } else {
                    stringResource(R.string.gig_meta_unpaid)
                }
                val meta = buildList {
                    gig.distanceKm?.let { add(stringResource(R.string.gig_meta_distance, it.toString())) }
                    add(priceText)
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
