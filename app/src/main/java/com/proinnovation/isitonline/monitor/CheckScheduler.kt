package com.proinnovation.isitonline.monitor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.proinnovation.isitonline.IsItOnlineApp
import java.util.Calendar

object CheckScheduler {

    private const val ACTION_CHECK = "com.proinnovation.isitonline.CHECK_ALARM"
    private const val MAIN_REQUEST_CODE = 1
    private const val RETRY_REQUEST_CODE = 2

    private const val DAYTIME_INTERVAL_MS = 30 * 60 * 1000L
    private const val OVERNIGHT_INTERVAL_MS = 2 * 60 * 60 * 1000L
    private const val RETRY_INTERVAL_MS = 60 * 1000L

    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences(IsItOnlineApp.PREFS_NAME, Context.MODE_PRIVATE)
        val dayStart = prefs.getInt("day_start_hour", 7)
        val dayEnd = prefs.getInt("day_end_hour", 22)
        val intervalMs = if (isDaytime(dayStart, dayEnd)) DAYTIME_INTERVAL_MS else OVERNIGHT_INTERVAL_MS
        schedule(context, MAIN_REQUEST_CODE, intervalMs)
    }

    fun scheduleRetry(context: Context) {
        schedule(context, RETRY_REQUEST_CODE, RETRY_INTERVAL_MS)
    }

    fun cancelRetry(context: Context) {
        cancel(context, RETRY_REQUEST_CODE)
    }

    fun cancel(context: Context) {
        cancel(context, MAIN_REQUEST_CODE)
        cancel(context, RETRY_REQUEST_CODE)
    }

    private fun schedule(context: Context, requestCode: Int, delayMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fall back to inexact alarm if permission not granted
            am.setWindow(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                5 * 60 * 1000L,
                pendingIntent(context, requestCode)
            )
            return
        }
        am.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMs,
            pendingIntent(context, requestCode)
        )
    }

    private fun cancel(context: Context, requestCode: Int) {
        val pi = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, CheckAlarmReceiver::class.java).apply { action = ACTION_CHECK },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
    }

    private fun pendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, CheckAlarmReceiver::class.java).apply { action = ACTION_CHECK },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun isDaytime(dayStart: Int, dayEnd: Int): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in dayStart until dayEnd
    }
}
