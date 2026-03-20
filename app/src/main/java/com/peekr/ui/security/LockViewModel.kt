package com.peekr.ui.security

import androidx.lifecycle.ViewModel
import com.peekr.security.SecurityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LockUiState(
    val enteredPin: String = "",
    val error: String? = null,
    val isUnlocked: Boolean = false
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val securityManager: SecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    fun addDigit(digit: String) {
        if (_uiState.value.enteredPin.length >= 4) return
        _uiState.update { it.copy(enteredPin = it.enteredPin + digit, error = null) }
        if (_uiState.value.enteredPin.length == 4) verifyPin()
    }

    fun deleteDigit() {
        _uiState.update {
            it.copy(
                enteredPin = it.enteredPin.dropLast(1),
                error = null
            )
        }
    }

    fun verifyPin() {
        val pin = _uiState.value.enteredPin
        if (pin.length < 4) return

        if (securityManager.verifyPin(pin)) {
            securityManager.onUnlocked()
            _uiState.update { it.copy(isUnlocked = true, error = null) }
        } else {
            _uiState.update { it.copy(enteredPin = "", error = "PIN غلط، حاول مرة تانية") }
        }
    }

    fun onBiometricSuccess() {
        securityManager.onUnlocked()
        _uiState.update { it.copy(isUnlocked = true) }
    }

    fun isBiometricEnabled() = securityManager.isBiometricEnabled()
}
