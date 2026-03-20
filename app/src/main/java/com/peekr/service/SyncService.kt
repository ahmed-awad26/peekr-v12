package com.peekr.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.peekr.core.logger.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {

    @Inject
    lateinit var logger: AppLogger

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.info("SyncService started")
        // هنضيف المزامنة الفعلية في الأجزاء القادمة
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        logger.info("SyncService destroyed")
    }
}
