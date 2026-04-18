package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onlyflows.OnlyFlowsApp
import com.onlyflows.data.preferences.AppPreferences
import com.onlyflows.data.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val fontSizeOptions = listOf(
    0.8f  to "XS",
    0.9f  to "S",
    1.0f  to "M",
    1.15f to "L",
    1.3f  to "XL",
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AppearanceViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs: AppPreferences = (app as OnlyFlowsApp).container.appPreferences

    val themeMode: StateFlow<ThemeMode> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val fontScale: StateFlow<Float> = prefs.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1.0f)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { prefs.setFontScale(scale) }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit = {},
    viewModel: AppearanceViewModel = viewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Apparence",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text     = "THÈME",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column {
                    ThemeOption(
                        label     = "Suivre le système",
                        selected  = themeMode == ThemeMode.SYSTEM,
                        showDivider = true,
                        onClick   = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    )
                    ThemeOption(
                        label     = "Clair",
                        selected  = themeMode == ThemeMode.LIGHT,
                        showDivider = true,
                        onClick   = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    )
                    ThemeOption(
                        label     = "Sombre",
                        selected  = themeMode == ThemeMode.DARK,
                        showDivider = false,
                        onClick   = { viewModel.setThemeMode(ThemeMode.DARK) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text     = "TAILLE DU TEXTE",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    fontSizeOptions.forEach { (scale, label) ->
                        val selected = fontScale == scale
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.setFontScale(scale) },
                            label    = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Aperçu: La taille du texte s'applique à toute l'application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick  = onClick,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier  = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color     = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
