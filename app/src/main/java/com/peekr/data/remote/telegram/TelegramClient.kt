package com.peekr.data.remote.telegram

import android.content.Context
import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.ApiKeyDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.AccountEntity
import com.peekr.data.local.entities.PostEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ============================================================
// Auth states
// ============================================================
sealed class TelegramAuthState {
    object Idle : TelegramAuthState()
    object LoadingTdlib : TelegramAuthState()
    object WaitingPhone : TelegramAuthState()
    data class WaitingCode(val hint: String = "") : TelegramAuthState()
    data class WaitingPassword(val hint: String = "") : TelegramAuthState()
    object Authorized : TelegramAuthState()
    data class WaitingPairing(
        val pairCode: String,
        val botUsername: String,
        val deepLink: String
    ) : TelegramAuthState()
    data class PairingSuccess(
        val userId: Long, val username: String, val firstName: String
    ) : TelegramAuthState()
    data class Error(val message: String) : TelegramAuthState()
}

// ============================================================
// TelegramClient
// ============================================================
@Singleton
class TelegramClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyDao: ApiKeyDao,
    private val accountDao: AccountDao,
    private val postDao: PostDao,
    private val logger: AppLogger
) {
    private val _authState = MutableStateFlow<TelegramAuthState>(TelegramAuthState.Idle)
    val authState: StateFlow<TelegramAuthState> = _authState

    // TDLight client (lazy init)
    private var tdClient: Any? = null        // it.tdlight.client.SimpleTelegramClient
    private var tdClientBuilder: Any? = null // it.tdlight.client.SimpleTelegramClientBuilder

    // Bot pairing
    private var activePairCode: String? = null
    private var pairingToken: String? = null

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ================================================================
    // initialize
    // ================================================================
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val botToken = apiKeyDao.getApiKeyByPlatform("telegram_bot")?.keyValue
            val apiId    = apiKeyDao.getApiKeyByPlatform("telegram_id")?.keyValue?.toIntOrNull()
            val apiHash  = apiKeyDao.getApiKeyByPlatform("telegram_hash")?.keyValue

            when {
                // Phone login — TDLight is in Gradle dependencies
                apiId != null && !apiHash.isNullOrBlank() -> {
                    initTdLight(apiId, apiHash)
                }

                // Bot token only
                !botToken.isNullOrBlank() -> {
                    if (verifyBotToken(botToken)) {
                        _authState.value = TelegramAuthState.Authorized
                        true
                    } else {
                        _authState.value = TelegramAuthState.Error("Bot Token غير صحيح")
                        false
                    }
                }

                else -> {
                    _authState.value = TelegramAuthState.Error(
                        "أضف API ID + API Hash في مفاتيح API\n\nأو أضف Bot Token للقنوات العامة فقط"
                    )
                    false
                }
            }
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error("خطأ: ${e.message}")
            false
        }
    }

    // ================================================================
    // TDLight init — uses it.tdlight library from Gradle
    // ================================================================
    private suspend fun initTdLight(apiId: Int, apiHash: String): Boolean {
        return try {
            _authState.value = TelegramAuthState.LoadingTdlib

            // Init TDLight
            val initClass   = Class.forName("it.tdlight.Init")
            initClass.getMethod("init").invoke(null)

            // Create settings
            val settingsClass = Class.forName("it.tdlight.client.TDLibSettings")
            val createMethod  = settingsClass.getMethod("create",
                Class.forName("it.tdlight.client.APIToken"))
            val tokenClass    = Class.forName("it.tdlight.client.APIToken")
            val token         = tokenClass.getDeclaredConstructor(Int::class.java, String::class.java)
                                    .newInstance(apiId, apiHash)
            val settings      = createMethod.invoke(null, token) as Any

            // Set database directory
            try {
                val dbDir = java.io.File(context.filesDir, "tdlib_db")
                dbDir.mkdirs()
                settingsClass.getMethod("setDatabaseDirectoryPath",
                    java.nio.file.Path::class.java)
                    .invoke(settings, dbDir.toPath())
            } catch (_: Exception) { }

            // Create client
            val factoryClass  = Class.forName("it.tdlight.client.SimpleTelegramClientFactory")
            val factory       = factoryClass.getDeclaredConstructor().newInstance()
            val builderMethod = factoryClass.getMethod("builder",
                Class.forName("it.tdlight.client.TDLibSettings"))
            val builder = builderMethod.invoke(factory, settings)
            tdClientBuilder  = builder

            // Register auth handler
            val builderClass = builder.javaClass
            val addHandlerMethod = try {
                builderClass.getMethod("addUpdateHandler",
                    Class.forName("java.lang.Class"),
                    Class.forName("it.tdlight.client.GenericUpdateHandler"))
            } catch (_: Exception) { null }

            // Build client
            val buildMethod = builderClass.methods.firstOrNull { it.name == "build" }
            if (buildMethod != null) {
                tdClient = buildMethod.invoke(builder)
                setupAuthHandlers()
            }

            _authState.value = TelegramAuthState.WaitingPhone
            true
        } catch (e: ClassNotFoundException) {
            logger.warning("TDLight not in classpath — check build.gradle", "telegram")
            _authState.value = TelegramAuthState.Error(
                "تعذر تحميل TDLight\n\nتأكد من اتصال الإنترنت وإعادة بناء المشروع (Build → Clean Project)"
            )
            false
        } catch (e: Exception) {
            logger.error("TDLight init error: ${e.message}", "telegram", e)
            _authState.value = TelegramAuthState.Error("خطأ TDLight: ${e.message?.take(100)}")
            false
        }
    }

    // ================================================================
    // Auth handlers — listens for TDLight auth updates
    // ================================================================
    private fun setupAuthHandlers() {
        val client = tdClient ?: return
        try {
            // Use reflection to add listener for TdApi.UpdateAuthorizationState
            val clientClass = client.javaClass
            val methods = clientClass.methods

            // Try to add authorization state handler
            val authHandlerProxy = java.lang.reflect.Proxy.newProxyInstance(
                javaClass.classLoader,
                tryGetHandlerInterface()
            ) { _, _, args ->
                val update = args?.getOrNull(0) ?: return@newProxyInstance null
                handleAuthUpdate(update)
                null
            }

            methods.firstOrNull { it.name == "addAuthorizationStateHandler" || it.name == "addUpdateHandler" }
                ?.let { method ->
                    try { method.invoke(client, authHandlerProxy) }
                    catch (_: Exception) { }
                }
        } catch (_: Exception) { }
    }

    private fun tryGetHandlerInterface(): Array<Class<*>> {
        return try {
            arrayOf(Class.forName("it.tdlight.client.GenericUpdateHandler"))
        } catch (_: Exception) {
            try { arrayOf(Class.forName("it.tdlight.client.ResultHandler")) }
            catch (_: Exception) { arrayOf() }
        }
    }

    private fun handleAuthUpdate(update: Any) {
        val className = update.javaClass.simpleName
        when {
            "WaitingPhoneNumber" in className || "WaitPhoneNumber" in className ->
                _authState.value = TelegramAuthState.WaitingPhone

            "WaitingCode" in className || "WaitCode" in className ->
                _authState.value = TelegramAuthState.WaitingCode()

            "WaitingPassword" in className || "WaitPassword" in className -> {
                val hint = try {
                    update.javaClass.getField("passwordHint").get(update) as? String ?: ""
                } catch (_: Exception) { "" }
                _authState.value = TelegramAuthState.WaitingPassword(hint)
            }

            "Ready" in className -> {
                _authState.value = TelegramAuthState.Authorized
                savePhoneSession()
            }
        }
    }

    // ================================================================
    // Phone auth — send to TDLight
    // ================================================================
    suspend fun sendPhoneNumber(phone: String) = withContext(Dispatchers.IO) {
        if (phone.isBlank()) {
            _authState.value = TelegramAuthState.Error("أدخل رقم الهاتف")
            return@withContext
        }

        val client = tdClient
        if (client == null) {
            // TDLight not initialized — try to init first
            val apiId   = apiKeyDao.getApiKeyByPlatform("telegram_id")?.keyValue?.toIntOrNull()
            val apiHash = apiKeyDao.getApiKeyByPlatform("telegram_hash")?.keyValue
            if (apiId != null && !apiHash.isNullOrBlank()) {
                initTdLight(apiId, apiHash)
                return@withContext // state will change to WaitingPhone
            }
            // Fall back to bot pairing
            startPairing()
            return@withContext
        }

        try {
            // Send SetAuthenticationPhoneNumber via reflection
            val tdapiClass = Class.forName("it.tdlight.jni.TdApi\$SetAuthenticationPhoneNumber")
            val req = tdapiClass.getDeclaredConstructor().newInstance()
            tdapiClass.getField("phoneNumber").set(req, phone.trim())

            val sendMethod = client.javaClass.methods
                .firstOrNull { it.name == "send" || it.name == "execute" }
            sendMethod?.invoke(client, req)
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error("فشل إرسال رقم الهاتف: ${e.message?.take(80)}")
        }
    }

    suspend fun sendCode(code: String) = withContext(Dispatchers.IO) {
        if (code.isBlank()) return@withContext
        val client = tdClient ?: return@withContext
        try {
            val cls = Class.forName("it.tdlight.jni.TdApi\$CheckAuthenticationCode")
            val req = cls.getDeclaredConstructor().newInstance()
            cls.getField("code").set(req, code.trim())
            client.javaClass.methods.firstOrNull { it.name == "send" || it.name == "execute" }
                ?.invoke(client, req)
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error("كود خاطئ: ${e.message?.take(60)}")
        }
    }

    suspend fun sendPassword(password: String) = withContext(Dispatchers.IO) {
        if (password.isBlank()) return@withContext
        val client = tdClient ?: return@withContext
        try {
            val cls = Class.forName("it.tdlight.jni.TdApi\$CheckAuthenticationPassword")
            val req = cls.getDeclaredConstructor().newInstance()
            cls.getField("password").set(req, password)
            client.javaClass.methods.firstOrNull { it.name == "send" || it.name == "execute" }
                ?.invoke(client, req)
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error("كلمة المرور خاطئة: ${e.message?.take(60)}")
        }
    }

    private fun savePhoneSession() {
        try {
            runBlocking(Dispatchers.IO) {
                accountDao.insertAccount(AccountEntity(
                    platformId  = "telegram",
                    accountName = "حساب تليجرام",
                    isConnected = true,
                    connectedAt = System.currentTimeMillis(),
                    extraData   = "tdlight_session"
                ))
            }
        } catch (_: Exception) { }
    }

    // ================================================================
    // Bot pairing (for public channels, no phone needed)
    // ================================================================
    suspend fun startPairing(): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = apiKeyDao.getApiKeyByPlatform("telegram_bot")?.keyValue
            if (token.isNullOrBlank()) {
                _authState.value = TelegramAuthState.Error(
                    "أضف Bot Token أو API ID + Hash في مفاتيح API"
                )
                return@withContext false
            }
            val meJson = get("https://api.telegram.org/bot$token/getMe")
            if (meJson?.optBoolean("ok", false) != true) {
                _authState.value = TelegramAuthState.Error("Bot Token خاطئ")
                return@withContext false
            }
            val botUsername = meJson.optJSONObject("result")?.optString("username", "") ?: ""
            val code = generateCode()
            activePairCode = code
            pairingToken   = token
            _authState.value = TelegramAuthState.WaitingPairing(
                pairCode    = code,
                botUsername = botUsername,
                deepLink    = "https://t.me/$botUsername?start=pair_$code"
            )
            true
        } catch (e: Exception) {
            _authState.value = TelegramAuthState.Error("خطأ: ${e.message}")
            false
        }
    }

    suspend fun checkPairingConfirmed(): Boolean = withContext(Dispatchers.IO) {
        val code  = activePairCode ?: return@withContext false
        val token = pairingToken   ?: return@withContext false
        try {
            val json = get("https://api.telegram.org/bot$token/getUpdates?limit=50&timeout=0")
                ?: return@withContext false
            if (!json.optBoolean("ok", false)) return@withContext false
            val arr = json.optJSONArray("result") ?: return@withContext false
            for (i in 0 until arr.length()) {
                val msg  = arr.getJSONObject(i).optJSONObject("message") ?: continue
                if (!msg.optString("text", "").contains("pair_$code")) continue
                val from      = msg.optJSONObject("from") ?: continue
                val userId    = from.optLong("id")
                val username  = from.optString("username", "")
                val firstName = from.optString("first_name", "User")
                val chatId    = msg.optJSONObject("chat")?.optLong("id") ?: userId

                accountDao.insertAccount(AccountEntity(
                    platformId  = "telegram_user",
                    accountName = if (username.isNotBlank()) "@$username" else firstName,
                    isConnected = true,
                    connectedAt = System.currentTimeMillis(),
                    extraData   = """{"userId":$userId,"chatId":$chatId}"""
                ))

                get("https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId" +
                    "&text=${java.net.URLEncoder.encode("مرحباً $firstName! ✅ تم ربط Peekr", "UTF-8")}")

                val lastId = arr.getJSONObject(arr.length()-1).optLong("update_id")
                get("https://api.telegram.org/bot$token/getUpdates?offset=${lastId+1}&limit=1")

                _authState.value = TelegramAuthState.PairingSuccess(userId, username, firstName)
                activePairCode = null; pairingToken = null
                return@withContext true
            }
            false
        } catch (_: Exception) { false }
    }

    // ================================================================
    // Sync channels via Bot API
    // ================================================================
    suspend fun syncChats(): Result<Int> = withContext(Dispatchers.IO) {
        val token = apiKeyDao.getApiKeyByPlatform("telegram_bot")?.keyValue
            ?: return@withContext Result.failure(Exception("أضف Bot Token في مفاتيح API"))
        val channels = accountDao.getAllAccountsByPlatformSync("telegram")
        if (channels.isEmpty())
            return@withContext Result.failure(Exception("أضف قنوات تليجرام من ربط الحسابات"))
        var total = 0
        channels.forEach { ch ->
            try { total += fetchChannelPosts(token, ch.accountName) }
            catch (e: Exception) { logger.error("sync: ${ch.accountName}", "telegram", e) }
        }
        Result.success(total)
    }

    private suspend fun fetchChannelPosts(token: String, input: String): Int {
        val chatId = input.trim()
            .removePrefix("https://t.me/").removePrefix("t.me/")
            .let { if (!it.startsWith("@") && !it.startsWith("-")) "@$it" else it }
        val chatJson = get("https://api.telegram.org/bot$token/getChat?chat_id=$chatId") ?: return 0
        if (!chatJson.optBoolean("ok", false)) return 0
        val chatObj  = chatJson.optJSONObject("result") ?: return 0
        val title    = chatObj.optString("title", chatId)
        val username = chatObj.optString("username", chatId.trimStart('@'))
        val updates  = get("https://api.telegram.org/bot$token/getUpdates?limit=50&allowed_updates=[\"channel_post\"]") ?: return 0
        if (!updates.optBoolean("ok", false)) return 0
        val arr = updates.optJSONArray("result") ?: return 0
        var count = 0
        for (i in 0 until arr.length()) {
            val post = arr.getJSONObject(i).optJSONObject("channel_post") ?: continue
            if (post.optJSONObject("chat")?.optString("username","").orEmpty().lowercase() != username.lowercase()) continue
            val msgId   = post.optLong("message_id")
            val text    = post.optString("text", post.optString("caption","")).trim()
            val date    = post.optLong("date") * 1000L
            val url     = "https://t.me/$username/$msgId"
            if (text.isBlank() || postDao.existsByUrl(url)) continue
            val photo   = post.optJSONArray("photo")
            val photoUrl = if (photo != null && photo.length() > 0)
                resolveFile(token, photo.getJSONObject(photo.length()-1).optString("file_id")) else null
            postDao.insertPost(PostEntity("telegram", username, title, text, photoUrl, url, date))
            count++
        }
        if (arr.length() > 0) {
            val last = arr.getJSONObject(arr.length()-1).optLong("update_id")
            get("https://api.telegram.org/bot$token/getUpdates?offset=${last+1}&limit=1")
        }
        return count
    }

    // ================================================================
    // Helpers
    // ================================================================
    suspend fun verifyBotToken(token: String) = withContext(Dispatchers.IO) {
        try { get("https://api.telegram.org/bot$token/getMe")?.optBoolean("ok",false)==true }
        catch (_: Exception) { false }
    }

    private suspend fun resolveFile(token: String, fileId: String): String? {
        return try {
            val j = get("https://api.telegram.org/bot$token/getFile?file_id=$fileId") ?: return null
            val p = j.optJSONObject("result")?.optString("file_path") ?: return null
            "https://api.telegram.org/file/bot$token/$p"
        } catch (_: Exception) { null }
    }

    private fun get(url: String): JSONObject? = try {
        val r = http.newCall(Request.Builder().url(url).build()).execute()
        r.body?.string()?.let { JSONObject(it) }
    } catch (_: Exception) { null }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { chars[SecureRandom().nextInt(chars.length)] }.joinToString("")
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try { tdClient?.javaClass?.getMethod("close")?.invoke(tdClient) } catch (_: Exception) {}
        tdClient = null
        accountDao.deleteAccountByPlatform("telegram")
        accountDao.deleteAccountByPlatform("telegram_user")
        _authState.value = TelegramAuthState.Idle
    }

    fun isAuthorized() = _authState.value is TelegramAuthState.Authorized
                      || _authState.value is TelegramAuthState.PairingSuccess
}
