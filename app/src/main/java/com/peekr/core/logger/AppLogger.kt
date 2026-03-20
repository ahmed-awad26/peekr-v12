package com.peekr.core.logger

import android.util.Log
import com.peekr.data.local.dao.LogDao
import com.peekr.data.local.entities.LogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    private val logDao: LogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun error(message: String, platformId: String? = null, throwable: Throwable? = null) {
        Log.e("Peekr", "[$platformId] $message", throwable)
        save("ERROR", message, platformId, throwable?.stackTraceToString())
    }

    fun warning(message: String, platformId: String? = null) {
        Log.w("Peekr", "[$platformId] $message")
        save("WARNING", message, platformId)
    }

    fun info(message: String, platformId: String? = null) {
        Log.i("Peekr", "[$platformId] $message")
        save("INFO", message, platformId)
    }

    private fun save(level: String, message: String, platformId: String?, stackTrace: String? = null) {
        scope.launch {
            logDao.insertLog(
                LogEntity(
                    level = level,
                    platformId = platformId,
                    message = message,
                    stackTrace = stackTrace
                )
            )
            // احتفظ بآخر 500 لوج بس
            logDao.deleteOldLogs(System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000))
        }
    }
}
