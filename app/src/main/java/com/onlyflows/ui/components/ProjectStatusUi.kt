package com.onlyflows.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.onlyflows.domain.model.ProjectStatus

// Single source of truth for how a ProjectStatus looks across the app (icon, not emoji).

val ProjectStatus.statusLabel: String get() = when (this) {
    ProjectStatus.NOT_STARTED -> "Not Started"
    ProjectStatus.IN_PROGRESS -> "In Progress"
    ProjectStatus.WAITING     -> "Waiting"
    ProjectStatus.DONE        -> "Done"
    ProjectStatus.PAUSED      -> "Paused"
}

val ProjectStatus.statusShortLabel: String get() = when (this) {
    ProjectStatus.NOT_STARTED -> "Not started"
    ProjectStatus.IN_PROGRESS -> "Active"
    ProjectStatus.WAITING     -> "Waiting"
    ProjectStatus.DONE        -> "Done"
    ProjectStatus.PAUSED      -> "Paused"
}

val ProjectStatus.statusIcon: ImageVector get() = when (this) {
    ProjectStatus.NOT_STARTED -> Icons.Outlined.RadioButtonUnchecked
    ProjectStatus.IN_PROGRESS -> Icons.Outlined.PlayCircle
    ProjectStatus.WAITING     -> Icons.Outlined.HourglassEmpty
    ProjectStatus.DONE        -> Icons.Outlined.CheckCircle
    ProjectStatus.PAUSED      -> Icons.Outlined.PauseCircle
}

val ProjectStatus.statusTint: Color @Composable get() {
    val dark = isSystemInDarkTheme()
    return when (this) {
        ProjectStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        ProjectStatus.IN_PROGRESS -> Color(0xFF4A90D9)
        ProjectStatus.WAITING     -> if (dark) Color(0xFFFFC94D) else Color(0xFFE8A200)
        ProjectStatus.DONE        -> if (dark) Color(0xFF81C995) else Color(0xFF2E9E5B)
        ProjectStatus.PAUSED      -> if (dark) Color(0xFFBDBDBD) else Color(0xFF757575)
    }
}
