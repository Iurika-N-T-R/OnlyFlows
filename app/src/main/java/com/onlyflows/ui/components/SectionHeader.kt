package com.onlyflows.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlyflows.ui.theme.OnlyFlowsTheme

@Composable
fun SectionHeader(
    title: String,
    emoji: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showMenu: Boolean = false,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 4.dp, top = 24.dp, bottom = 6.dp),
    ) {
        if (emoji != null) {
            Text(
                text  = "$emoji ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color      = accentColor,
            modifier   = Modifier.weight(1f),
        )
        if (showMenu) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector        = Icons.Outlined.MoreHoriz,
                    contentDescription = "Section menu",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun GroupHeader(
    title: String,
    painter: Painter? = null,
    emoji: String? = null,
    iconSize: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 8.dp),
    ) {
        when {
            painter != null -> {
                Icon(
                    painter            = painter,
                    contentDescription = null,
                    tint               = Color.Unspecified,
                    modifier           = Modifier.size(iconSize),
                )
                Spacer(Modifier.width(8.dp))
            }
            emoji != null -> {
                Text(text = "$emoji ", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            text  = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
    OnlyFlowsTheme {
        Column {
            SectionHeader(title = "Things to buy", accentColor = Color(0xFF4A90D9), showMenu = true)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)
            SectionHeader(title = "Planning", accentColor = Color(0xFF4A90D9), showMenu = true)
        }
    }
}
