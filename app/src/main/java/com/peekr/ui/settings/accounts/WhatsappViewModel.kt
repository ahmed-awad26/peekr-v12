package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.remote.whatsapp.WhatsappBridge
import com.peekr.data.remote.whatsapp.WhatsappState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhatsappViewModel @Inject constructor(
    private val whatsappBridge: WhatsappBridge
) : ViewModel() {

    val state: StateFlow<WhatsappState> = whatsappBridge.state

    fun connect() {
        viewModelScope.launch {
            whatsappBridge.connect()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            whatsappBridge.disconnect()
        }
    }
}
