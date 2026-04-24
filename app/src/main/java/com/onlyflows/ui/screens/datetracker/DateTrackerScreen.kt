package com.onlyflows.ui.screens.datetracker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.domain.model.DateTracker
import com.onlyflows.domain.model.DateTrackerCategory
import com.onlyflows.ui.components.EmptyState
import com.onlyflows.ui.components.FlowFab
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DateTrackerScreen(
    onOpenDrawer: () -> Unit        = {},
    viewModel: DateTrackerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (state.showAddSheet) {
        AddDateTrackerSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(DateTrackerEvent.HideAddSheet) },
        )
    }

    if (state.editingTracker != null) {
        AddDateTrackerSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(DateTrackerEvent.HideEditSheet) },
            isEdit    = true,
        )
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏳", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Dates & Comptes à rebours",
                            style      = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Outlined.Menu, "Menu")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FlowFab(onClick = { viewModel.onEvent(DateTrackerEvent.ShowAddSheet) })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->

        if (state.trackers.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.fillMaxSize().padding(padding),
            ) {
                EmptyState(
                    emoji    = "⏳",
                    title    = "Aucun suivi",
                    subtitle = "Ajoute un anniversaire, compte à rebours ou événement.",
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top    = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start  = 16.dp,
                    end    = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier            = Modifier.fillMaxSize(),
            ) {
                items(state.trackers, key = { it.id }) { tracker ->
                    TrackerCard(
                        tracker   = tracker,
                        onDelete  = { viewModel.onEvent(DateTrackerEvent.Delete(tracker.id)) },
                        onEdit    = { viewModel.onEvent(DateTrackerEvent.ShowEditSheet(tracker)) },
                        modifier  = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

// ── Tracker card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackerCard(
    tracker: DateTracker,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Refresh label every time the composition enters (covers midnight crossing)
    var now by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val next = LocalDate.now()
            if (next != now) now = next
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val label = remember(tracker, now) { computeLabel(tracker, now) }
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape    = RoundedCornerShape(14.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick      = {},
                onLongClick  = { showMenu = true },
            ),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji circle
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Text(tracker.emoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = tracker.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text  = categoryLabel(tracker.category) + if (tracker.isAnnual) " · annuel" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            // Date chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text     = tracker.targetDate.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                    style    = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text        = { Text("Modifier") },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                onClick     = { showMenu = false; onEdit() },
            )
            DropdownMenuItem(
                text      = { Text("Supprimer") },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick   = { showMenu = false; onDelete() },
            )
        }
    }
}

// ── Time label computation ────────────────────────────────────────────────────

internal fun computeLabel(tracker: DateTracker, now: LocalDate): String {
    return when {
        // ANNIVERSARY always shows elapsed age, regardless of isAnnual toggle
        tracker.category == DateTrackerCategory.ANNIVERSARY -> {
            val thisYearBirthday = tracker.targetDate.withYear(now.year)
            val nextBirthday = if (!thisYearBirthday.isAfter(now)) thisYearBirthday.plusYears(1) else thisYearBirthday
            val daysUntilNext = ChronoUnit.DAYS.between(now, nextBirthday)
            val period = Period.between(tracker.targetDate, now)
            buildString {
                if (period.years > 0) append("${period.years} an${if (period.years > 1) "s" else ""} ")
                if (period.months > 0) append("${period.months} mois ")
                append("${period.days} jour${if (period.days > 1) "s" else ""}")
                if (daysUntilNext in 0..7) append(" · 🎂 dans $daysUntilNext j")
            }.trim()
        }
        // Annual non-anniversary: countdown to next occurrence
        tracker.isAnnual -> {
            val thisYear = tracker.targetDate.withYear(now.year)
            val next     = if (!thisYear.isBefore(now)) thisYear else thisYear.plusYears(1)
            val days     = ChronoUnit.DAYS.between(now, next)
            when {
                days == 0L  -> "Aujourd'hui ! 🎉"
                days == 1L  -> "Demain"
                days < 30L  -> "Dans $days jours"
                else        -> {
                    val p = Period.between(now, next)
                    buildString {
                        if (p.months > 0) append("${p.months} mois ")
                        append("${p.days} jour${if (p.days > 1) "s" else ""}")
                    }.trim()
                }
            }
        }
        // One-shot countdown or elapsed
        else -> {
            val days = ChronoUnit.DAYS.between(now, tracker.targetDate)
            when {
                days == 0L  -> "Aujourd'hui ! 🎉"
                days == 1L  -> "Demain"
                days > 0L   -> {
                    val p = Period.between(now, tracker.targetDate)
                    buildString {
                        if (p.years > 0)  append("${p.years} an${if (p.years > 1) "s" else ""} ")
                        if (p.months > 0) append("${p.months} mois ")
                        append("${p.days} jour${if (p.days != 1) "s" else ""} restant${if (p.days != 1) "s" else ""}")
                    }.trim()
                }
                else -> {
                    val p = Period.between(tracker.targetDate, now)
                    buildString {
                        if (p.years > 0)  append("${p.years} an${if (p.years > 1) "s" else ""} ")
                        if (p.months > 0) append("${p.months} mois ")
                        append("${p.days} jour${if (p.days != 1) "s" else ""} passé${if (p.days != 1) "s" else ""}")
                    }.trim()
                }
            }
        }
    }
}

private fun categoryLabel(cat: DateTrackerCategory) = when (cat) {
    DateTrackerCategory.COUNTDOWN   -> "Compte à rebours"
    DateTrackerCategory.ANNIVERSARY -> "Anniversaire"
    DateTrackerCategory.EVENT       -> "Événement"
    DateTrackerCategory.HOLIDAY     -> "Fête"
    DateTrackerCategory.OTHER       -> "Autre"
}

// ── Add sheet ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDateTrackerSheet(
    state: DateTrackerUiState,
    onEvent: (DateTrackerEvent) -> Unit,
    onDismiss: () -> Unit,
    isEdit: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    // Text field: jj/mm/aaaa
    var dateText by remember(state.draftDate) {
        mutableStateOf(state.draftDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "")
    }
    fun commitDateText(raw: String) {
        runCatching {
            val d = java.time.LocalDate.parse(raw, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            onEvent(DateTrackerEvent.SetDraftDate(d))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.draftDate
                ?.let { java.time.ZoneOffset.UTC.let { tz -> it.atStartOfDay().toInstant(tz).toEpochMilli() } }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val date = java.time.Instant.ofEpochMilli(ms)
                            .atOffset(java.time.ZoneOffset.UTC).toLocalDate()
                        onEvent(DateTrackerEvent.SetDraftDate(date))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (isEdit) "Modifier le suivi" else "Nouveau suivi",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Fermer")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Emoji + Name row
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = state.draftEmoji,
                    onValueChange = { if (it.length <= 2) onEvent(DateTrackerEvent.SetDraftEmoji(it)) },
                    label         = { Text("Emoji") },
                    singleLine    = true,
                    modifier      = Modifier.width(80.dp),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value         = state.draftName,
                    onValueChange = { onEvent(DateTrackerEvent.SetDraftName(it)) },
                    label         = { Text("Nom") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Date: text field jj/mm/aaaa + calendar icon
            OutlinedTextField(
                value         = dateText,
                onValueChange = { v ->
                    dateText = v
                    if (v.length == 10) commitDateText(v)
                },
                label         = { Text("Date (jj/mm/aaaa)") },
                placeholder   = { Text("17/05/1999") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(18.dp))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )

            Spacer(Modifier.height(14.dp))

            // Category chips
            Text("Catégorie", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateTrackerCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = state.draftCategory == cat,
                        onClick  = { onEvent(DateTrackerEvent.SetDraftCategory(cat)) },
                        label    = { Text(categoryLabel(cat), style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Annual toggle
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Récurrent chaque année", modifier = Modifier.weight(1f))
                Switch(
                    checked         = state.draftIsAnnual,
                    onCheckedChange = { onEvent(DateTrackerEvent.SetDraftIsAnnual(it)) },
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = { onEvent(if (isEdit) DateTrackerEvent.SaveEdit else DateTrackerEvent.SaveDraft) },
                enabled  = state.draftName.isNotBlank() && state.draftDate != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isEdit) "Enregistrer" else "Ajouter")
            }
        }
    }
}
