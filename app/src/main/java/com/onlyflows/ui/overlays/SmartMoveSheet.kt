package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Project
import com.onlyflows.domain.model.TaskGroup
import com.onlyflows.ui.theme.ModalDark

/**
 * Extended move sheet for Inbox / Anytime / Someday screens.
 * Allows moving tasks to a scheduled group (Anytime, Someday, Inbox)
 * OR to a project/section.
 *
 * [hiddenGroup] — the current screen's group so we don't show it as an option.
 * Callback: onConfirm(scheduledGroup, projectId, sectionId)
 *   - scheduledGroup != null && projectId == null → set scheduledGroup
 *   - scheduledGroup == null && projectId == null → clear scheduledGroup (→ Inbox)
 *   - projectId != null → move to project
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMoveSheet(
    count: Int,
    projects: List<Project>,
    hiddenGroup: TaskGroup? = null,           // don't show the current view's group
    onConfirm: (scheduledGroup: TaskGroup?, projectId: String?, sectionId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedSection by remember { mutableStateOf<Project?>(null) }   // unused, kept for symmetry

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = ModalDark,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedProject != null) {
                        IconButton(
                            onClick  = { selectedProject = null },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (selectedProject == null)
                            "Move $count task${if (count > 1) "s" else ""}"
                        else
                            selectedProject!!.title,
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            if (selectedProject == null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                ) {
                    // ── Scheduled group options ───────────────────────────────
                    if (hiddenGroup != TaskGroup.ANYTIME) {
                        item(key = "anytime") {
                            SmartMoveRow(
                                emoji   = "✨",
                                color   = Color(0xFF9B59B6),
                                label   = "Anytime",
                                onClick = { onConfirm(TaskGroup.ANYTIME, null, null) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                    if (hiddenGroup != TaskGroup.SOMEDAY) {
                        item(key = "someday") {
                            SmartMoveRow(
                                emoji   = "🌊",
                                color   = Color(0xFF2980B9),
                                label   = "Someday",
                                onClick = { onConfirm(TaskGroup.SOMEDAY, null, null) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                    // Back to Inbox (only show from Anytime/Someday, not from Inbox itself)
                    if (hiddenGroup != null) {
                        item(key = "inbox") {
                            SmartMoveRow(
                                emoji   = "📥",
                                color   = Color(0xFF4A90D9),
                                label   = "Inbox",
                                onClick = { onConfirm(null, null, null) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }

                    // ── Divider label ─────────────────────────────────────────
                    item(key = "projects_header") {
                        Text(
                            text     = "PROJECTS",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }

                    // ── Projects ──────────────────────────────────────────────
                    items(projects, key = { it.id }) { project ->
                        SmartMoveRow(
                            emoji   = project.iconEmoji.ifBlank {
                                project.title.firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                            },
                            color   = runCatching {
                                Color(android.graphics.Color.parseColor(project.colorHex))
                            }.getOrDefault(Color(0xFF4A90D9)),
                            label   = project.title,
                            onClick = {
                                if (project.sections.isEmpty()) {
                                    onConfirm(null, project.id, null)
                                } else {
                                    selectedProject = project
                                }
                            },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                }
            } else {
                // ── Section picker for selected project ───────────────────────
                val project = selectedProject!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                ) {
                    item(key = "no_section") {
                        SmartMoveRow(
                            emoji   = "—",
                            color   = Color.White.copy(alpha = 0.3f),
                            label   = "No section",
                            onClick = { onConfirm(null, project.id, null) },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                    items(project.sections, key = { it.id }) { section ->
                        SmartMoveRow(
                            emoji   = "§",
                            color   = Color.White.copy(alpha = 0.3f),
                            label   = section.title,
                            onClick = { onConfirm(null, project.id, section.id) },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMoveRow(
    emoji: String,
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color),
        ) {
            Text(emoji, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}
