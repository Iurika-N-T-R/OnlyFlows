package com.onlyflows.ui.overlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlyflows.domain.model.Tag
import com.onlyflows.domain.model.TaskPriority
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkMoreSheet(
    selectedCount: Int,
    tags: List<Tag>,
    onStar: (Boolean) -> Unit,
    onSetPriority: (TaskPriority) -> Unit,
    onAddTag: (String) -> Unit,
    onSchedule: (LocalDate?) -> Unit,
    onTrackTask: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var page by remember { mutableStateOf(Page.ROOT) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
            ) {
                if (page != Page.ROOT) {
                    IconButton(onClick = { page = Page.ROOT }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                }
                Text(
                    text = when (page) {
                        Page.ROOT     -> "$selectedCount tâche${if (selectedCount > 1) "s" else ""} sélectionnée${if (selectedCount > 1) "s" else ""}"
                        Page.PRIORITY -> "Priorité"
                        Page.TAG      -> "Ajouter un tag"
                        Page.SCHEDULE -> "Planifier"
                    },
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            when (page) {
                Page.ROOT -> RootPage(
                    onStar        = onStar,
                    onPriority    = { page = Page.PRIORITY },
                    onTag         = { page = Page.TAG },
                    onSchedule    = { page = Page.SCHEDULE },
                    onTrackTask   = if (selectedCount == 1) onTrackTask else null,
                )
                Page.PRIORITY -> PriorityPage(onSetPriority)
                Page.TAG      -> TagPage(tags, onAddTag)
                Page.SCHEDULE -> SchedulePage(onSchedule)
            }
        }
    }
}

private enum class Page { ROOT, PRIORITY, TAG, SCHEDULE }

// ── Root page ─────────────────────────────────────────────────────────────────

@Composable
private fun RootPage(
    onStar: (Boolean) -> Unit,
    onPriority: () -> Unit,
    onTag: () -> Unit,
    onSchedule: () -> Unit,
    onTrackTask: (() -> Unit)? = null,
) {
    Column {
        SheetRow(icon = Icons.Outlined.Star,         label = "Marquer comme favori")    { onStar(true) }
        SheetRow(icon = Icons.Outlined.StarBorder,   label = "Retirer des favoris")     { onStar(false) }
        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant,
        )
        SheetRow(icon = Icons.Outlined.Flag,         label = "Priorité",   trailing = true, onClick = onPriority)
        SheetRow(icon = Icons.Outlined.Label,        label = "Ajouter un tag", trailing = true, onClick = onTag)
        SheetRow(icon = Icons.Outlined.CalendarMonth, label = "Planifier",  trailing = true, onClick = onSchedule)
        if (onTrackTask != null) {
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outlineVariant,
            )
            SheetRow(icon = Icons.Outlined.Timer, label = "Démarrer le minuteur", onClick = onTrackTask)
        }
    }
}

// ── Priority page ─────────────────────────────────────────────────────────────

@Composable
private fun PriorityPage(onSetPriority: (TaskPriority) -> Unit) {
    Column {
        listOf(
            TaskPriority.HIGH   to ("Haute"   to androidx.compose.ui.graphics.Color(0xFFEF5350)),
            TaskPriority.MEDIUM to ("Moyenne" to androidx.compose.ui.graphics.Color(0xFFFF9800)),
            TaskPriority.LOW    to ("Basse"   to androidx.compose.ui.graphics.Color(0xFF42A5F5)),
            TaskPriority.NONE   to ("Aucune"  to androidx.compose.ui.graphics.Color(0xFF9E9E9E)),
        ).forEach { (priority, pair) ->
            val (label, color) = pair
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSetPriority(priority) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(16.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// ── Tag page ──────────────────────────────────────────────────────────────────

@Composable
private fun TagPage(tags: List<Tag>, onAddTag: (String) -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (tags.isEmpty()) {
            Text(
                text     = "Aucun tag disponible",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            tags.forEach { tag ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddTag(tag.id) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// ── Schedule page ─────────────────────────────────────────────────────────────

@Composable
private fun SchedulePage(onSchedule: (LocalDate?) -> Unit) {
    val today    = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val nextWeek = today.plusWeeks(1)

    Column {
        SheetRow(icon = Icons.Outlined.Today,          label = "Aujourd'hui")  { onSchedule(today) }
        SheetRow(icon = Icons.Outlined.WbSunny,        label = "Demain")       { onSchedule(tomorrow) }
        SheetRow(icon = Icons.Outlined.DateRange,      label = "La semaine prochaine") { onSchedule(nextWeek) }
        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant,
        )
        SheetRow(icon = Icons.Outlined.EventBusy, label = "Supprimer la date") { onSchedule(null) }
    }
}

// ── Shared row ────────────────────────────────────────────────────────────────

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    trailing: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing) {
            Icon(
                imageVector        = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}
