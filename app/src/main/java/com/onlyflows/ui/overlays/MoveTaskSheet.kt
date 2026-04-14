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
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
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
import com.onlyflows.domain.model.Section
import com.onlyflows.ui.theme.ModalDark

private enum class MovePage { ROOT, LIST, LIST_SECTIONS, SECTION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveTaskSheet(
    count: Int,
    projects: List<Project>,
    currentProjectSections: List<Section> = emptyList(),
    onConfirm: (projectId: String?, sectionId: String?) -> Unit,
    onMoveToSection: ((sectionId: String?) -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    // If no current-project sections, skip ROOT page and go straight to list picker
    val startPage = if (currentProjectSections.isEmpty() || onMoveToSection == null)
        MovePage.LIST else MovePage.ROOT
    var page            by remember { mutableStateOf(startPage) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedSection by remember { mutableStateOf<Section?>(null) }

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
            // ── Header ──
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (page != MovePage.ROOT && page != startPage) {
                        IconButton(
                            onClick  = {
                                page = when (page) {
                                    MovePage.LIST_SECTIONS -> { selectedProject = null; selectedSection = null; MovePage.LIST }
                                    MovePage.SECTION       -> MovePage.ROOT
                                    else                   -> startPage
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = when (page) {
                            MovePage.ROOT          -> "Déplacer $count tâche${if (count > 1) "s" else ""}"
                            MovePage.LIST          -> "Choisir une liste"
                            MovePage.LIST_SECTIONS -> selectedProject?.title ?: "Choisir une section"
                            MovePage.SECTION       -> "Choisir une section"
                        },
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = Color.White.copy(alpha = 0.7f))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            when (page) {
                // ── Root: choose mode ──
                MovePage.ROOT -> {
                    Column {
                        MoveOptionRow(
                            label   = "Déplacer dans une section",
                            onClick = { page = MovePage.SECTION },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        MoveOptionRow(
                            label   = "Déplacer dans une autre liste",
                            onClick = { page = MovePage.LIST },
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                    }
                }

                // ── Move to section (same project) ──
                MovePage.SECTION -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        item(key = "no_section") {
                            MoveRow(
                                color = Color.White.copy(alpha = 0.3f),
                                label = "Sans section",
                                initial = "—",
                                isProject = false,
                                selected = false,
                                onClick = { onMoveToSection?.invoke(null) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                        items(currentProjectSections, key = { it.id }) { section ->
                            MoveRow(
                                color     = Color.White.copy(alpha = 0.3f),
                                label     = section.title,
                                initial   = "§",
                                isProject = false,
                                selected  = false,
                                onClick   = { onMoveToSection?.invoke(section.id) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                }

                // ── Move to list: project picker ──
                MovePage.LIST -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        item(key = "inbox") {
                            MoveRow(
                                color = Color(0xFF4A90D9), label = "Inbox",
                                initial = "✉", isProject = true, selected = false,
                                onClick = { onConfirm(null, null) },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                        items(projects, key = { it.id }) { project ->
                            MoveRow(
                                color     = runCatching { Color(android.graphics.Color.parseColor(project.colorHex)) }.getOrDefault(Color(0xFF4A90D9)),
                                label     = project.title,
                                initial   = project.iconEmoji.ifBlank { project.title.firstOrNull()?.uppercaseChar()?.toString() ?: "P" },
                                isProject = true,
                                selected  = false,
                                onClick   = {
                                    if (project.sections.isEmpty()) onConfirm(project.id, null)
                                    else { selectedProject = project; page = MovePage.LIST_SECTIONS }
                                },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                }

                // ── Move to list → section picker ──
                MovePage.LIST_SECTIONS -> {
                    val proj = selectedProject!!
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                        item(key = "no_section") {
                            MoveRow(
                                color = Color.White.copy(alpha = 0.3f), label = "Sans section",
                                initial = "—", isProject = false,
                                selected = selectedSection == null,
                                onClick = { selectedSection = null },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                        items(proj.sections, key = { it.id }) { section ->
                            MoveRow(
                                color = Color.White.copy(alpha = 0.3f), label = section.title,
                                initial = "§", isProject = false,
                                selected = selectedSection?.id == section.id,
                                onClick = { selectedSection = section },
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick  = { onConfirm(proj.id, selectedSection?.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    ) { Text("Déplacer ici") }
                }
            }
        }
    }
}

@Composable
private fun MoveOptionRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyLarge,
            color    = Color.White,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint     = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun MoveRow(
    color: Color,
    label: String,
    initial: String,
    isProject: Boolean,
    selected: Boolean,
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
                .clip(if (isProject) CircleShape else RoundedCornerShape(6.dp))
                .background(color),
        ) {
            Text(
                text  = initial,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
