package com.onlyflows.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    emoji: String? = null,
    /** Preferred over [emoji]: an icon takes the theme's colour and renders the same everywhere. */
    icon: ImageVector? = null,
    iconTint: Color? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        when {
            icon != null -> Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier           = Modifier.size(44.dp),
            )
            emoji != null -> Text(
                text  = emoji,
                style = MaterialTheme.typography.displayMedium,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text      = subtitle,
            style     = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
