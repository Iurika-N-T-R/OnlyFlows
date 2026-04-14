package com.onlyflows.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.ui.theme.OnlyFlowsTheme

@Composable
fun SelectionBar(
    selectedCount: Int,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    onArchive: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color  = Color(0xFF1A2235),
            shape  = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(vertical = 12.dp),
            ) {
                SelectionAction(
                    icon  = Icons.AutoMirrored.Outlined.ArrowForward,
                    label = "Move",
                    onClick = onMove,
                )
                if (onArchive != null) {
                    SelectionAction(
                        icon  = Icons.Outlined.Archive,
                        label = "Archive",
                        onClick = onArchive,
                    )
                }
                SelectionAction(
                    icon  = Icons.Outlined.DeleteOutline,
                    label = "Delete",
                    onClick = onDelete,
                )
                SelectionAction(
                    icon  = Icons.Outlined.MoreHoriz,
                    label = "More",
                    onClick = onMore,
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick  = onClick,
        colors   = ButtonDefaults.textButtonColors(contentColor = Color.White),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview
@Composable
private fun SelectionBarPreview() {
    OnlyFlowsTheme {
        Box(Modifier.fillMaxWidth()) {
            SelectionBar(
                selectedCount = 2,
                onMove = {}, onDelete = {}, onMore = {},
            )
        }
    }
}
