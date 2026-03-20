package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.remote.telegram.TelegramAuthState
import com.peekr.data.remote.telegram.TelegramClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TelegramLoginViewModel @Inject constructor(
    private val telegramClient: TelegramClient
) : ViewModel() {

    val authState: StateFlow<TelegramAuthState> = telegramClient.authState

    private var pollingJob: Job? = null

    init { initialize() }

    fun initialize() {
        viewModelScope.launch { telegramClient.initialize() }
    }

    // Start the bot pairing flow
    fun startPairing() {
        viewModelScope.launch {
            val started = telegramClient.startPairing()
            if (started) startPolling()
        }
    }

    // Poll every 3 sec to check if user tapped Start in Telegram
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            repeat(60) {          // poll for max 3 minutes (60 × 3s)
                delay(3000)
                val confirmed = telegramClient.checkPairingConfirmed()
                if (confirmed) return@launch
            }
        }
    }

    fun stopPolling() { pollingJob?.cancel() }

    fun sendPhone(phone: String) {
        viewModelScope.launch { telegramClient.sendPhoneNumber(phone) }
    }

    fun sendCode(code: String) {
        viewModelScope.launch { telegramClient.sendCode(code) }
    }

    fun sendPassword(password: String) {
        viewModelScope.launch { telegramClient.sendPassword(password) }
    }
}
