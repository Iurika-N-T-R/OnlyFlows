package com.onlyflows.wear.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import com.onlyflows.wear.data.WearSettings

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var showCompleted by remember { mutableStateOf(WearSettings.showCompleted(context)) }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = rememberScalingLazyListState()) {
        item {
            Text(
                "Réglages",
                style = MaterialTheme.typography.title3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        item {
            ToggleChip(
                checked = showCompleted,
                onCheckedChange = {
                    showCompleted = it
                    WearSettings.setShowCompleted(context, it)
                },
                label = { Text("Afficher les terminées") },
                toggleControl = { Text(if (showCompleted) "✓" else "—") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
