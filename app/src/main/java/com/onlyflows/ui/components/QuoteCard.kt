package com.onlyflows.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onlyflows.data.local.Quote

@Composable
fun QuoteCard(
    quote: Quote,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            modifier          = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "“${quote.text}”",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (quote.author.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text      = "— ${quote.author}",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier  = Modifier.fillMaxWidth(),
                    )
                }
            }
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = "Dismiss",
                    modifier           = Modifier.size(14.dp),
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
    }
}
