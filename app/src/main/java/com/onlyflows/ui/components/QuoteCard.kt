package com.onlyflows.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.onlyflows.data.local.Quote

/** The day's quote as a quiet pull quote: an accent bar and text, no box competing with the tasks. */
@Composable
fun QuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(vertical = 2.dp)) {
            Text(
                text  = quote.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (quote.author.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = quote.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}
