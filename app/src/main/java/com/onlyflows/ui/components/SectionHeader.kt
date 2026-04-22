package com.onlyflows.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlyflows.ui.theme.OnlyFlowsTheme
import androidx.compose.foundation.layout.BoxScope

@Composable
fun SectionHeader(
    title: String,
    emoji: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showMenu: Boolean = false,
    onMenuClick: () -> Unit = {},
    menuContent: @Composable BoxScope.() -> Unit = {},
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    onAddTask: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val chevronAngle by animateFloatAsState(targetValue = if (isCollapsed) 0f else 90f, label = "chevron")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onToggleCollapse != null) Modifier.clickable { onToggleCollapse() } else Modifier)
            .padding(start = 20.dp, end = 4.dp, top = 24.dp, bottom = 6.dp),
    ) {
        if (onToggleCollapse != null) {
            Icon(
                imageVector        = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint               = accentColor.copy(alpha = 0.7f),
                modifier           = Modifier.size(18.dp).rotate(chevronAngle),
            )
            Spacer(Modifier.width(4.dp))
        }
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
        if (onAddTask != null) {
            IconButton(onClick = onAddTask, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector        = Icons.Outlined.Add,
                    contentDescription = "Add task to section",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
        if (showMenu) {
            Box {
                IconButton(onClick = { onMenuClick() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector        = Icons.Outlined.MoreHoriz,
                        contentDescription = "Section menu",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier           = Modifier.size(18.dp),
                    )
                }
                menuContent()
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
