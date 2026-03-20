package com.peekr.data.remote.whatsapp

import android.content.Context
import com.peekr.core.logger.AppLogger
import com.peekr.data.local.dao.AccountDao
import com.peekr.data.local.dao.PostDao
import com.peekr.data.local.entities.AccountEntity
import com.peekr.data.local.entities.PostEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class WhatsappState {
    object Idle : WhatsappState()
    object Connecting : WhatsappState()
    data class QrReady(val qrBase64: String) : WhatsappState()
    object Connected : WhatsappState()
    data class Error(val message: String) : WhatsappState()
    object Disconnected : WhatsappState()
}

@Singleton
class WhatsappBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postDao: PostDao,
    private val accountDao: AccountDao,
    private val logger: AppLogger
) {
    private val _state = MutableStateFlow<WhatsappState>(WhatsappState.Idle)
    val state: StateFlow<WhatsappState> = _state

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    // Bridge Server بيشتغل على Node.js محلي على الجهاز
    // بنستخدم Termux أو embedded Node
    private val BRIDGE_URL = "ws://localhost:3001"

    // ==============================
    // بدء الاتصال وطلب QR
    // ==============================
    suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            _state.value = WhatsappState.Connecting
            logger.info("واتساب: جاري الاتصال بالـ Bridge", "whatsapp")

            val request = Request.Builder().url(BRIDGE_URL).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    logger.info("واتساب: اتصل بالـ Bridge", "whatsapp")
                    webSocket.send("""{"action":"getQR"}""")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleBridgeMessage(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    logger.error("واتساب: فشل الاتصال بالـ Bridge", "whatsapp", t)
                    _state.value = WhatsappState.Error("تأكد إن الـ Bridge شغال على الجهاز")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _state.value = WhatsappState.Disconnected
                }
            })
        } catch (e: Exception) {
            logger.error("خطأ في الاتصال بواتساب", "whatsapp", e)
            _state.value = WhatsappState.Error(e.message ?: "خطأ غير معروف")
        }
    }

    // ==============================
    // معالجة رسائل الـ Bridge
    // ==============================
    private fun handleBridgeMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.getString("type")) {
                "qr" -> {
                    val qrData = json.getString("data")
                    _state.value = WhatsappState.QrReady(qrData)
                    logger.info("واتساب: QR Code جاهز", "whatsapp")
                }
                "ready" -> {
                    _state.value = WhatsappState.Connected
                    logger.info("واتساب: تم الاتصال بنجاح", "whatsapp")
                    saveConnection()
                }
                "message" -> {
                    handleNewMessage(json.getJSONObject("data"))
                }
                "disconnected" -> {
                    _state.value = WhatsappState.Disconnected
                    logger.warning("واتساب: انقطع الاتصال", "whatsapp")
                }
                "error" -> {
                    val errorMsg = json.optString("message", "خطأ غير معروف")
                    logger.error("واتساب Bridge Error: $errorMsg", "whatsapp")
                    _state.value = WhatsappState.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            logger.error("خطأ في معالجة رسالة Bridge", "whatsapp", e)
        }
    }

    // ==============================
    // معالجة الرسائل الواردة
    // ==============================
    private fun handleNewMessage(data: JSONObject) {
        try {
            val from = data.optString("from", "")
            val body = data.optString("body", "")
            val timestamp = data.optLong("timestamp", System.currentTimeMillis() / 1000) * 1000
            val isGroup = data.optBoolean("isGroup", false)
            val senderName = data.optString("senderName", from)
            val chatName = data.optString("chatName", senderName)

            if (body.isEmpty()) return

            // احفظ الرسالة في الداتابيز
            GlobalScope.launch(Dispatchers.IO) {
                postDao.insertPost(
                    PostEntity(
                        platformId = "whatsapp",
                        sourceId = from,
                        sourceName = if (isGroup) "👥 $chatName" else chatName,
                        content = if (isGroup) "$senderName: $body" else body,
                        timestamp = timestamp
                    )
                )
                logger.info("واتساب: رسالة جديدة من $chatName", "whatsapp")
            }
        } catch (e: Exception) {
            logger.error("خطأ في معالجة رسالة واتساب", "whatsapp", e)
        }
    }

    // ==============================
    // حفظ حالة الاتصال
    // ==============================
    private fun saveConnection() {
        GlobalScope.launch(Dispatchers.IO) {
            accountDao.insertAccount(
                AccountEntity(
                    platformId = "whatsapp",
                    accountName = "واتساب",
                    isConnected = true,
                    connectedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // ==============================
    // قطع الاتصال
    // ==============================
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            webSocket?.send("""{"action":"logout"}""")
            webSocket?.close(1000, "User logout")
            webSocket = null
            accountDao.deleteAccountByPlatform("whatsapp")
            _state.value = WhatsappState.Idle
            logger.info("واتساب: تم قطع الاتصال", "whatsapp")
        } catch (e: Exception) {
            logger.error("خطأ في قطع الاتصال بواتساب", "whatsapp", e)
        }
    }

    fun isConnected() = _state.value == WhatsappState.Connected
}
