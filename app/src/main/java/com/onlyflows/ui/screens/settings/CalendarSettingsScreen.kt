package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.calendar.CalendarRepository
import com.onlyflows.data.preferences.CalendarPreferences
import com.onlyflows.domain.model.CalendarAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── State ─────────────────────────────────────────────────────────────────────

data class CalendarSettingsUiState(
    val calendars: List<CalendarAccount> = emptyList(),
    val disabledIds: Set<String>         = emptySet(),
    val hasPermission: Boolean           = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CalendarSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container      = (app as OnlyFlowsApp).container
    private val calendarRepo: CalendarRepository   = container.calendarRepository
    private val calendarPrefs: CalendarPreferences = container.calendarPreferences

    private val _uiState = MutableStateFlow(CalendarSettingsUiState())
    val uiState: StateFlow<CalendarSettingsUiState> = _uiState.asStateFlow()

    init {
        val hasPermission = calendarRepo.hasPermission()
        val calendars     = if (hasPermission) calendarRepo.getCalendars() else emptyList()
        _uiState.update { it.copy(hasPermission = hasPermission, calendars = calendars) }

        viewModelScope.launch {
            calendarPrefs.disabledCalendarIds.collectLatest { disabled ->
                _uiState.update { it.copy(disabledIds = disabled) }
            }
        }
    }

    fun setEnabled(calendarId: String, enabled: Boolean) {
        viewModelScope.launch {
            calendarPrefs.setEnabled(calendarId, enabled)
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Calendars",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (!state.hasPermission) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
            ) {
                Text(
                    text  = "Calendar access is not granted. Enable it in app settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        val grouped = remember(state.calendars) {
            state.calendars.groupBy { it.accountName }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top    = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            grouped.forEach { (accountName, calendars) ->
                item(key = "account_$accountName") {
                    Text(
                        text     = accountName,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                items(calendars, key = { "cal_${it.id}" }) { calendar ->
                    val enabled  = calendar.id !in state.disabledIds
                    val dotColor = runCatching {
                        Color(android.graphics.Color.parseColor(calendar.colorHex))
                    }.getOrElse { MaterialTheme.colorScheme.primary }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Surface(
                            shape    = MaterialTheme.shapes.extraSmall,
                            color    = dotColor,
                            modifier = Modifier.size(12.dp),
                        ) {}
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text     = calendar.displayName,
                            style    = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked         = enabled,
                            onCheckedChange = { viewModel.setEnabled(calendar.id, it) },
                        )
                    }
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}
