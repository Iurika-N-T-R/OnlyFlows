package com.onlyflows.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlyflows.R
import com.onlyflows.ui.theme.ModalDark
import com.onlyflows.ui.theme.OnlyFlowsTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialDate: LocalDate? = null,
    initialDueTime: LocalTime? = null,
    initialEndTime: LocalTime? = null,
    showTimeOption: Boolean = false,
    // Legacy callback for callers that don't need time
    onDateSelected: ((LocalDate?) -> Unit)? = null,
    // Full callback for callers that need date + time
    onResult: ((date: LocalDate?, dueTime: LocalTime?, endTime: LocalTime?) -> Unit)? = null,
    // Called when user taps "Add Reminder" — saves date first, then opens reminder picker
    onAddReminder: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var currentMonth  by remember { mutableStateOf(YearMonth.now()) }
    var isAllDay      by remember { mutableStateOf(initialDueTime == null) }

    val dueTimeState = rememberTimePickerState(
        initialHour   = initialDueTime?.hour ?: 9,
        initialMinute = initialDueTime?.minute ?: 0,
        is24Hour      = true,
    )
    val endTimeState = rememberTimePickerState(
        initialHour   = initialEndTime?.hour ?: 10,
        initialMinute = initialEndTime?.minute ?: 0,
        is24Hour      = true,
    )
    var showEndTime by remember { mutableStateOf(initialEndTime != null) }

    fun fireResult(date: LocalDate?) {
        if (onResult != null) {
            val dt = if (!showTimeOption || isAllDay) null
                     else LocalTime.of(dueTimeState.hour, dueTimeState.minute)
            val et = if (!showTimeOption || isAllDay || !showEndTime) null
                     else LocalTime.of(endTimeState.hour, endTimeState.minute)
            onResult(date, dt, et)
        } else {
            onDateSelected?.invoke(date)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = ModalDark,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text       = "When?",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                if (showTimeOption && selectedDate != null) {
                    // "Done" when time option is active
                    TextButton(onClick = { fireResult(selectedDate) }) {
                        Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // ── Shortcuts ────────────────────────────────────────────────────
            DateShortcut(
                painter    = painterResource(R.drawable.ic_nav_today),
                label      = "Today",
                isSelected = selectedDate == LocalDate.now(),
                onClick    = {
                    selectedDate = LocalDate.now()
                    if (!showTimeOption) fireResult(LocalDate.now())
                },
            )
            DateShortcut(
                painter    = painterResource(R.drawable.ic_nav_evening),
                label      = "This Evening",
                isSelected = false,
                onClick    = {
                    selectedDate = LocalDate.now()
                    if (!showTimeOption) fireResult(LocalDate.now())
                },
            )

            Spacer(Modifier.height(12.dp))

            // ── Calendar ──────────────────────────────────────────────────────
            CalendarView(
                currentMonth  = currentMonth,
                selectedDate  = selectedDate,
                onMonthChange = { currentMonth = it },
                onDaySelected = { date ->
                    selectedDate = date
                    if (!showTimeOption) fireResult(date)
                },
            )

            Spacer(Modifier.height(4.dp))

            // ── Someday ───────────────────────────────────────────────────────
            DateShortcut(
                painter    = painterResource(R.drawable.ic_nav_someday),
                label      = "Someday",
                isSelected = false,
                onClick    = { fireResult(null); onDismiss() },
            )

            // ── Time option (deadline picker only) ────────────────────────────
            if (showTimeOption && selectedDate != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color    = Color.White.copy(alpha = 0.08f),
                )

                // All Day toggle
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = "All Day",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Switch(
                        checked         = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor  = Color.White,
                            checkedTrackColor  = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }

                if (!isAllDay) {
                    Spacer(Modifier.height(8.dp))

                    // Start time
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) {
                        Text(
                            text     = "Start",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.width(52.dp),
                        )
                        TimeInput(
                            state  = dueTimeState,
                            colors = TimePickerDefaults.colors(
                                timeSelectorSelectedContainerColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.07f),
                                timeSelectorSelectedContentColor     = Color.White,
                                timeSelectorUnselectedContentColor   = Color.White.copy(alpha = 0.5f),
                            ),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // End time toggle
                    if (!showEndTime) {
                        TextButton(
                            onClick  = { showEndTime = true },
                            modifier = Modifier.padding(start = 12.dp),
                        ) {
                            Text(
                                text  = "+ Add End Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.45f),
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        ) {
                            Text(
                                text     = "End",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.width(52.dp),
                            )
                            TimeInput(
                                state  = endTimeState,
                                colors = TimePickerDefaults.colors(
                                    timeSelectorSelectedContainerColor   = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    timeSelectorUnselectedContainerColor = Color.White.copy(alpha = 0.07f),
                                    timeSelectorSelectedContentColor     = Color.White,
                                    timeSelectorUnselectedContentColor   = Color.White.copy(alpha = 0.5f),
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }

            // ── Add Reminder ─────────────────────────────────────────────────
            // Only offered when a caller can actually handle it. Rendered unconditionally
            // it was a dead control that just applied a null date and closed the sheet.
            if (onAddReminder != null) {
                TextButton(
                    onClick  = {
                        fireResult(selectedDate)   // save the date selection first
                        onAddReminder.invoke()     // then open the reminder picker
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Text(
                        text  = "+ Add Reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateShortcut(
    painter: Painter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(painter = painter, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.titleMedium,
            color    = Color.White,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CalendarView(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    onMonthChange: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
) {
    val today    = LocalDate.now()
    val dayNames = DayOfWeek.values().map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        // Day names header
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { day ->
                Text(
                    text      = day,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        val firstDay    = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val startOffset = firstDay.dayOfWeek.value - 1 // Mon=0, Tue=1, …, Sun=6

        val allCells = buildList {
            repeat(startOffset) { add(null) }
            for (d in 1..daysInMonth) add(currentMonth.atDay(d))
        }

        LazyVerticalGrid(
            columns           = GridCells.Fixed(7),
            modifier          = Modifier.fillMaxWidth().heightIn(max = 240.dp),
            userScrollEnabled = false,
        ) {
            items(allCells) { date ->
                if (date == null) {
                    Box(Modifier.aspectRatio(1f))
                } else {
                    val isSelected = date == selectedDate
                    val isToday    = date == today
                    val isPast     = date < today
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday    -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else       -> Color.Transparent
                                }
                            )
                            .clickable { onDaySelected(date) },
                    ) {
                        Text(
                            text       = date.dayOfMonth.toString(),
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color      = when {
                                isSelected -> Color.White
                                isPast     -> Color.White.copy(alpha = 0.3f)
                                else       -> Color.White
                            },
                            textAlign  = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Month navigation
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                Text("< Prev", color = Color.White.copy(alpha = 0.5f))
            }
            Text(
                text  = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
            TextButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                Text("Next >", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DatePickerModalPreview() {
    OnlyFlowsTheme {
        DatePickerModal(
            initialDate    = LocalDate.now(),
            showTimeOption = true,
            onDateSelected = {},
            onDismiss      = {},
        )
    }
}
