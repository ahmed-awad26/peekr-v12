package com.peekr.ui.settings.apikeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.entities.ApiKeyEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PlatformStatusItem(
    val id: String,
    val name: String,
    val color: Long,
    val hasKey: Boolean,
    val isConnected: Boolean?,   // null = not tested
    val isTesting: Boolean = false
)

data class ApiKeysUiState(
    val keyValues: Map<String, String> = emptyMap(),
    val platformStatuses: List<PlatformStatusItem> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

private interface YoutubeTestApi {
    @GET("channels")
    suspend fun testKey(
        @Query("part") part: String = "id",
        @Query("id") id: String = "UCVHFbw7woebKtGBsxKtjHKg",
        @Query("key") apiKey: String
    ): Map<String, Any>
}

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val apiKeyDao: ApiKeyDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiKeysUiState())
    val uiState: StateFlow<ApiKeysUiState> = _uiState.asStateFlow()

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val ytApi: YoutubeTestApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YoutubeTestApi::class.java)
    }

    init { loadKeys() }

    // ==============================
    // Load from DB
    // ==============================
    private fun loadKeys() {
        viewModelScope.launch {
            apiKeyDao.getAllApiKeys().collect { keys ->
                val map = keys.associate { it.platformId to it.keyValue }
                _uiState.update { it.copy(keyValues = map, platformStatuses = buildStatuses(map)) }
            }
        }
    }

    private fun buildStatuses(keyMap: Map<String, String>): List<PlatformStatusItem> = listOf(
        PlatformStatusItem("telegram", "تليجرام", 0xFF0088CC,
            hasKey = keyMap.containsKey("telegram_bot") ||
                (keyMap.containsKey("telegram_id") && keyMap.containsKey("telegram_hash")),
            isConnected = null),
        PlatformStatusItem("youtube", "يوتيوب", 0xFFFF0000,
            hasKey = keyMap.containsKey("youtube"),
            isConnected = null),
        PlatformStatusItem("facebook", "فيسبوك", 0xFF1877F2,
            hasKey = keyMap.containsKey("facebook"),
            isConnected = null),
        PlatformStatusItem("rss", "RSS", 0xFFFF6600,
            hasKey = true, isConnected = true)  // RSS doesn't need a key
    )

    fun updateKeyValue(id: String, value: String) {
        _uiState.update { s ->
            s.copy(keyValues = s.keyValues + (id to value), savedSuccess = false)
        }
    }

    // ==============================
    // Save all keys to DB
    // ==============================
    fun saveAllKeys() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                _uiState.value.keyValues.forEach { (id, value) ->
                    if (value.isNotBlank()) {
                        apiKeyDao.insertApiKey(ApiKeyEntity(platformId = id, keyName = id, keyValue = value.trim()))
                    }
                }
                _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
                delay(200)
                // Rebuild statuses from saved values
                val saved = apiKeyDao.getAllApiKeysSync()
                val map = saved.associate { it.platformId to it.keyValue }
                _uiState.update { it.copy(platformStatuses = buildStatuses(map)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    // ==============================
    // Test connection — called when tapping a platform chip
    // ==============================
    fun testConnection(platformId: String) {
        viewModelScope.launch {
            // Mark as testing
            _uiState.update { s ->
                s.copy(platformStatuses = s.platformStatuses.map {
                    if (it.id == platformId) it.copy(isTesting = true, isConnected = null) else it
                })
            }
            val result = when (platformId) {
                "youtube"  -> testYouTube()
                "telegram" -> testTelegram()
                "facebook" -> testFacebook()
                "rss"      -> true
                else       -> false
            }
            // Update result
            _uiState.update { s ->
                s.copy(platformStatuses = s.platformStatuses.map {
                    if (it.id == platformId) it.copy(isTesting = false, isConnected = result) else it
                })
            }
        }
    }

    // ——— YouTube test ———
    private suspend fun testYouTube(): Boolean = withContext(Dispatchers.IO) {
        try {
            // First try from current UI values, then from DB
            val key = (_uiState.value.keyValues["youtube"]
                ?: apiKeyDao.getApiKeyByPlatform("youtube")?.keyValue)
                ?.takeIf { it.isNotBlank() } ?: return@withContext false

            ytApi.testKey(apiKey = key)
            true
        } catch (e: Exception) {
            // 400 means key is valid but query error — still means key works
            val msg = e.message ?: ""
            !msg.contains("403") && !msg.contains("400 is not valid")
        }
    }

    // ——— Telegram test ———
    private suspend fun testTelegram(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = (_uiState.value.keyValues["telegram_bot"]
                ?: apiKeyDao.getApiKeyByPlatform("telegram_bot")?.keyValue)
                ?.takeIf { it.isNotBlank() }

            if (!token.isNullOrBlank()) {
                // Test bot token
                val req = Request.Builder()
                    .url("https://api.telegram.org/bot$token/getMe")
                    .build()
                val resp = http.newCall(req).execute()
                val body = resp.body?.string() ?: return@withContext false
                return@withContext JSONObject(body).optBoolean("ok", false)
            }

            // Fallback: check if id+hash present and valid format
            val id   = _uiState.value.keyValues["telegram_id"]
                ?: apiKeyDao.getApiKeyByPlatform("telegram_id")?.keyValue
            val hash = _uiState.value.keyValues["telegram_hash"]
                ?: apiKeyDao.getApiKeyByPlatform("telegram_hash")?.keyValue
            !id.isNullOrBlank() && !hash.isNullOrBlank() && id.toIntOrNull() != null && hash.length >= 20
        } catch (_: Exception) { false }
    }

    // ——— Facebook test ———
    private suspend fun testFacebook(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = (_uiState.value.keyValues["facebook"]
                ?: apiKeyDao.getApiKeyByPlatform("facebook")?.keyValue)
                ?.takeIf { it.isNotBlank() } ?: return@withContext false

            val req = Request.Builder()
                .url("https://graph.facebook.com/me?access_token=$token")
                .build()
            val resp = http.newCall(req).execute()
            val body = resp.body?.string() ?: return@withContext false
            !body.contains("\"error\"")
        } catch (_: Exception) { false }
    }
}
