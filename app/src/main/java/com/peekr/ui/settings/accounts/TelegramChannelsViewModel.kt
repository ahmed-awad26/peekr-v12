package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.entities.AccountEntity
import com.peekr.data.remote.telegram.TelegramClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

sealed class ChannelVerifyState {
    object Idle : ChannelVerifyState()
    object Checking : ChannelVerifyState()
    data class OK(val title: String) : ChannelVerifyState()
    object Fail : ChannelVerifyState()
    object NoBotToken : ChannelVerifyState()
}

@HiltViewModel
class TelegramChannelsViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val apiKeyDao: ApiKeyDao,
    private val telegramClient: TelegramClient
) : ViewModel() {

    val channels = accountDao.getAllAccountsByPlatform("telegram")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _botConnected = MutableStateFlow(telegramClient.isAuthorized())
    val botConnectedFlow: StateFlow<Boolean> = _botConnected.asStateFlow()

    init {
        viewModelScope.launch {
            // Re-check bot status on open
            telegramClient.initialize()
            _botConnected.value = telegramClient.isAuthorized()
        }
    }

    private val _verifications = MutableStateFlow<Map<Long, ChannelVerifyState>>(emptyMap())
    val verifications: StateFlow<Map<Long, ChannelVerifyState>> = _verifications.asStateFlow()

    val botConnected: Boolean
        get() = telegramClient.isAuthorized()

    fun addChannel(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            val clean = username.trim()
                .removePrefix("https://t.me/")
                .removePrefix("t.me/")
            val id = accountDao.insertAccount(
                AccountEntity(
                    platformId  = "telegram",
                    accountName = if (clean.startsWith("@")) clean else "@$clean",
                    isConnected = true,
                    connectedAt = System.currentTimeMillis()
                )
            )
            verifyChannel(id, clean)
        }
    }

    fun removeChannel(account: AccountEntity) {
        viewModelScope.launch {
            accountDao.deleteAccountById(account.id)
            _verifications.update { it - account.id }
        }
    }

    fun verifyChannel(accountId: Long, username: String) {
        viewModelScope.launch {
            val token = apiKeyDao.getApiKeyByPlatform("telegram_bot")?.keyValue
            if (token.isNullOrBlank()) {
                _verifications.update { it + (accountId to ChannelVerifyState.NoBotToken) }
                return@launch
            }
            _verifications.update { it + (accountId to ChannelVerifyState.Checking) }
            val result = withContext(Dispatchers.IO) {
                try {
                    val clean = username.trim()
                        .removePrefix("https://t.me/")
                        .removePrefix("t.me/")
                    val chatId = if (!clean.startsWith("@")) "@$clean" else clean
                    val client = OkHttpClient()
                    val req = Request.Builder()
                        .url("https://api.telegram.org/bot$token/getChat?chat_id=$chatId")
                        .build()
                    val resp = client.newCall(req).execute()
                    val json = JSONObject(resp.body?.string() ?: "{}")
                    if (json.optBoolean("ok", false)) {
                        val title = json.optJSONObject("result")?.optString("title", chatId) ?: chatId
                        ChannelVerifyState.OK(title)
                    } else {
                        ChannelVerifyState.Fail
                    }
                } catch (_: Exception) { ChannelVerifyState.Fail }
            }
            _verifications.update { it + (accountId to result) }
        }
    }

    fun verifyAll() {
        viewModelScope.launch {
            channels.value.forEach { ch ->
                verifyChannel(ch.id, ch.accountName)
            }
        }
    }
}
