package com.peekr.ui.security

import androidx.lifecycle.ViewModel
import com.peekr.security.SecurityManager
import com.peekr.security.SecuritySettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SecuritySettingsUiState(
    val settings: SecuritySettings = SecuritySettings(),
    val biometricAvailable: Boolean = false,
    val showSetPinDialog: Boolean = false,
    val isChangingPin: Boolean = false,
    val pinError: String? = null
)

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecuritySettingsUiState())
    val uiState: StateFlow<SecuritySettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                settings = securityManager.getSettings(),
                biometricAvailable = securityManager.isBiometricAvailable()
            )
        }
    }

    fun toggleLock(enabled: Boolean) {
        securityManager.saveLockEnabled(enabled)
        loadSettings()
    }

    fun setLockAfter(seconds: Int) {
        securityManager.saveLockAfterSeconds(seconds)
        loadSettings()
    }

    fun setBiometric(enabled: Boolean) {
        securityManager.saveBiometricEnabled(enabled)
        loadSettings()
    }

    fun showSetPinDialog() {
        _uiState.update { it.copy(showSetPinDialog = true, isChangingPin = false, pinError = null) }
    }

    fun showChangePinDialog() {
        _uiState.update { it.copy(showSetPinDialog = true, isChangingPin = true, pinError = null) }
    }

    fun hidePinDialog() {
        _uiState.update { it.copy(showSetPinDialog = false, pinError = null) }
    }

    fun setPin(pin: String) {
        if (securityManager.setPin(pin)) {
            _uiState.update { it.copy(showSetPinDialog = false, pinError = null) }
            loadSettings()
        } else {
            _uiState.update { it.copy(pinError = "الـ PIN لازم يكون 4 أرقام على الأقل") }
        }
    }
}
