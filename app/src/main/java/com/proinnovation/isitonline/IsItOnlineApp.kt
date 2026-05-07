package com.proinnovation.isitonline

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.proinnovation.isitonline.data.db.AppDatabase
import com.proinnovation.isitonline.data.repository.SiteRepository
import com.proinnovation.isitonline.monitor.RetryTracker

class IsItOnlineApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: SiteRepository
        private set
    lateinit var retryTracker: RetryTracker
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = SiteRepository(database.siteDao())
        retryTracker = RetryTracker(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Site Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when monitored sites go offline"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "site_alerts"
        const val PREFS_NAME = "isitonline_prefs"
    }
}
