package com.onlyflows.wear.presentation

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.onlyflows.wear.data.Paths
import com.onlyflows.wear.data.WearBridge
import com.onlyflows.wear.data.WearList
import com.onlyflows.wear.data.parseLists
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MenuViewModel(app: Application) : AndroidViewModel(app) {
    private val _lists = MutableStateFlow<List<WearList>>(emptyList())
    val lists = _lists.asStateFlow()

    fun load() = viewModelScope.launch {
        _lists.value = parseLists(WearBridge.request(getApplication(), Paths.GET_LISTS))
    }
}

@Composable
fun MenuScreen(
    onPick: (type: String, id: String, label: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MenuViewModel = viewModel(),
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = rememberScalingLazyListState()) {
        item {
            Text(
                "Listes",
                style = MaterialTheme.typography.title3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
        }
        items(lists.size) { i ->
            val l = lists[i]
            Chip(
                onClick = { onPick(l.type, l.id, l.title) },
                label = { Text(l.title) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Chip(
                onClick = onOpenSettings,
                label = { Text("Réglages") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
