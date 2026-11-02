package com.onlyflows.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    label: String = "Completed",
    modifier: Modifier = Modifier,
) {
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label       = "completed_chevron",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier
                .size(18.dp)
                .rotate(chevronAngle),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        // Count badge
        Surface(
            color  = MaterialTheme.colorScheme.surfaceVariant,
            shape  = RoundedCornerShape(5.dp),
        ) {
            Text(
                text     = count.toString(),
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Quiet label that opens the finished part of a list ("COMPLETED · 3"): small caps under a
 * divider, so done tasks read as a separate, secondary block rather than another time slot.
 */
@Composable
fun DoneSectionLabel(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        Text(
            text          = "${label.uppercase()} · $count",
            style         = MaterialTheme.typography.labelMedium,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color         = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier      = Modifier.padding(top = 14.dp, bottom = 4.dp),
        )
    }
}
