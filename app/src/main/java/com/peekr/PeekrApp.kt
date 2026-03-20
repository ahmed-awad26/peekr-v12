package com.peekr

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.peekr.core.service.FeedSyncWorker
import com.peekr.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PeekrApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // تحديث الفيد كل 30 دقيقة
        FeedSyncWorker.schedule(this)
        // تحديث الويدجيز كل 15 دقيقة
        WidgetUpdateWorker.schedule(this)
    }
}
