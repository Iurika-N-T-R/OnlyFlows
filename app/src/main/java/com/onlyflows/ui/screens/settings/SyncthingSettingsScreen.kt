package com.onlyflows.ui.screens.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

// ponytail: Syncthing removed — placeholder until LAN sync settings screen is built
@Composable
fun SyncthingSettingsScreen(
    onBack: () -> Unit = {},
    vm: SyncthingViewModel = viewModel(),
) {
    Text("Syncthing removed — LAN sync coming soon")
}
