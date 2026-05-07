package com.proinnovation.isitonline.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.MainActivity
import com.proinnovation.isitonline.R
import com.proinnovation.isitonline.data.db.Site

class AlertNotifier(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)

    fun showFailureNotification(site: Site, checkType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("siteId", site.id)
        }
        val pi = PendingIntent.getActivity(
            context, notificationId(site.id, checkType), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, IsItOnlineApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_cloud_off)
            .setContentTitle("${site.label} $checkType offline")
            .setContentText("${site.url} failed 5 consecutive $checkType checks")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${site.url} has failed 5 consecutive $checkType checks. Tap to view.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .build()

        nm.notify(notificationId(site.id, checkType), notification)
    }

    fun cancelNotification(siteId: Long, checkType: String) {
        nm.cancel(notificationId(siteId, checkType))
    }

    private fun notificationId(siteId: Long, checkType: String): Int =
        siteId.toInt() * 10 + if (checkType == "HTTPS") 0 else 1
}
