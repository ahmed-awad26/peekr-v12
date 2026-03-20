package com.peekr.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.peekr.MainActivity
import com.peekr.R
import com.peekr.data.repository.FeedRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val feedRepository: FeedRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val results = feedRepository.syncAll()
            val totalNew = results.values.sumOf { it.getOrNull() ?: 0 }

            if (totalNew > 0) {
                showNotification(totalNew)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(count: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // إنشاء channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تحديثات Peekr",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "إشعارات المحتوى الجديد"
        }
        manager.createNotificationChannel(channel)

        // PendingIntent لفتح التطبيق
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Peekr")
            .setContentText("$count منشور جديد بينتظرك")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "peekr_sync"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "feed_sync_work"

        fun schedule(context: Context, intervalMinutes: Long = 30) {
            val request = PeriodicWorkRequestBuilder<FeedSyncWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // flex interval
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
