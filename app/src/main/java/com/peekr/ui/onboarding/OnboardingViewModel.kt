package com.peekr.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.core.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences
) : ViewModel() {

    val isOnboardingDone = prefs.isOnboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markDone() {
        viewModelScope.launch { prefs.markOnboardingDone() }
    }
}
