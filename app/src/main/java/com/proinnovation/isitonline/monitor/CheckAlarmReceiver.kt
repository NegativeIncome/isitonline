package com.proinnovation.isitonline.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proinnovation.isitonline.IsItOnlineApp
import kotlinx.coroutines.*

class CheckAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                MonitorRunner.runChecks(context)

                val app = context.applicationContext as IsItOnlineApp
                if (app.retryTracker.hasActiveRetries()) {
                    CheckScheduler.scheduleRetry(context)
                } else {
                    CheckScheduler.cancelRetry(context)
                    CheckScheduler.scheduleNext(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
