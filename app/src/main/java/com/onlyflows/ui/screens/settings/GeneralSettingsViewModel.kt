package com.onlyflows.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.onlyflows.OnlyFlowsApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeneralSettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = (app as OnlyFlowsApp).container.appPreferences

    val use24hFormat = prefs.use24hFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setUse24hFormat(enabled: Boolean) {
        viewModelScope.launch { prefs.setUse24hFormat(enabled) }
    }
}
