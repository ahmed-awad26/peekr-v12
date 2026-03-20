package com.peekr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.core.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val isDarkMode = prefs.isDarkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val useSystemTheme = prefs.useSystemTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isEnglish = prefs.isEnglish
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(dark) }
    }

    fun setUseSystemTheme(use: Boolean) {
        viewModelScope.launch { prefs.setUseSystemTheme(use) }
    }

    fun setEnglish(english: Boolean) {
        viewModelScope.launch { prefs.setEnglish(english) }
    }
}
