package com.peekr.ui.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.entities.AccountEntity
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

// حالة التحقق من القناة
sealed class ChannelValidation {
    object Idle : ChannelValidation()
    object Checking : ChannelValidation()
    data class Valid(val channelName: String) : ChannelValidation()
    object Invalid : ChannelValidation()
    object NoApiKey : ChannelValidation()
}

// minimal YouTube API for validation
private interface YoutubeValidateApi {
    @GET("channels")
    suspend fun getChannel(
        @Query("part") part: String = "snippet",
        @Query("forHandle") forHandle: String? = null,
        @Query("id") id: String? = null,
        @Query("key") apiKey: String
    ): YoutubeChannelResponse
}

data class YoutubeChannelResponse(
    val items: List<YoutubeChannelItem> = emptyList()
)
data class YoutubeChannelItem(
    val id: String,
    val snippet: YoutubeChannelSnippet
)
data class YoutubeChannelSnippet(val title: String = "")

@HiltViewModel
class YoutubeViewModel @Inject constructor(
    private val accountDao: AccountDao,
    private val apiKeyDao: ApiKeyDao
) : ViewModel() {

    val channels = accountDao.getAllAccountsByPlatform("youtube")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // حالة التحقق لكل رابط (id -> validation)
    private val _validations = MutableStateFlow<Map<Long, ChannelValidation>>(emptyMap())
    val validations: StateFlow<Map<Long, ChannelValidation>> = _validations.asStateFlow()

    private val validateApi: YoutubeValidateApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeValidateApi::class.java)
    }

    fun addChannel(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val id = accountDao.insertAccount(
                AccountEntity(
                    platformId = "youtube",
                    accountName = url.trim(),
                    isConnected = true,
                    connectedAt = System.currentTimeMillis()
                )
            )
            // ابدأ التحقق تلقائياً بعد الإضافة
            validateChannel(id, url.trim())
        }
    }

    fun removeChannel(account: AccountEntity) {
        viewModelScope.launch {
            accountDao.deleteAccountById(account.id)
            _validations.update { it - account.id }
        }
    }

    fun updateChannel(account: AccountEntity, newUrl: String) {
        viewModelScope.launch {
            accountDao.insertAccount(account.copy(accountName = newUrl.trim()))
            // تحقق من الرابط الجديد
            validateChannel(account.id, newUrl.trim())
        }
    }

    fun validateChannel(accountId: Long, url: String) {
        viewModelScope.launch {
            val apiKey = apiKeyDao.getApiKeyByPlatform("youtube")?.keyValue
            if (apiKey.isNullOrBlank()) {
                _validations.update { it + (accountId to ChannelValidation.NoApiKey) }
                return@launch
            }

            _validations.update { it + (accountId to ChannelValidation.Checking) }

            val result = withContext(Dispatchers.IO) {
                try {
                    val handle = extractHandle(url)
                    val response = if (handle.startsWith("UC")) {
                        validateApi.getChannel(id = handle, apiKey = apiKey)
                    } else {
                        validateApi.getChannel(forHandle = "@${handle.trimStart('@')}", apiKey = apiKey)
                    }
                    if (response.items.isNotEmpty()) {
                        ChannelValidation.Valid(response.items[0].snippet.title)
                    } else {
                        ChannelValidation.Invalid
                    }
                } catch (e: Exception) {
                    ChannelValidation.Invalid
                }
            }
            _validations.update { it + (accountId to result) }
        }
    }

    private fun extractHandle(url: String): String {
        return when {
            url.contains("@") -> url.substringAfterLast("@").substringBefore("/").substringBefore("?")
            url.contains("/c/") -> url.substringAfterLast("/c/").substringBefore("/")
            url.contains("/channel/") -> url.substringAfterLast("/channel/").substringBefore("/")
            url.startsWith("UC") -> url
            else -> url.trim()
        }
    }
}
