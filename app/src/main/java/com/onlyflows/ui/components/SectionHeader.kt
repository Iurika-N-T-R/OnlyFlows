package com.onlyflows.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlyflows.ui.theme.FlowGreen
import com.onlyflows.ui.theme.FlowRed
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
    /** Tasks done / tasks in this section — rendered as a pill next to the title. */
    doneCount: Int? = null,
    totalCount: Int? = null,
    /** An unfinished task in the section is past its date: the pill turns red. */
    hasOverdue: Boolean = false,
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
        // Title + pill take all the room left, so + and … always sit at the right edge.
        // (A weighted title next to a weighted spacer split the room in half, and the
        // buttons moved with the length of the title.)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = accentColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                modifier   = Modifier.weight(1f, fill = false),
            )
            if (doneCount != null && totalCount != null && totalCount > 0) {
                Spacer(Modifier.width(8.dp))
                SectionCountBadge(doneCount, totalCount, hasOverdue)
            }
        }
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

/** `done / total` for a section: green once everything is done, red while a task is overdue, grey otherwise. */
@Composable
private fun SectionCountBadge(done: Int, total: Int, hasOverdue: Boolean) {
    val allDone = done >= total
    val tint = when {
        allDone    -> FlowGreen
        hasOverdue -> FlowRed
        else       -> null
    }
    Surface(
        color = tint?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            if (allDone) {
                Icon(
                    imageVector        = Icons.Outlined.Check,
                    contentDescription = null,
                    tint               = FlowGreen,
                    modifier           = Modifier.size(11.dp),
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text       = "$done/$total",
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
