package com.proinnovation.isitonline.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.notification.AlertNotifier
import kotlinx.coroutines.*

class CheckAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as IsItOnlineApp
                val sites = app.repository.getEnabledSites()
                val checker = NetworkChecker()
                val notifier = AlertNotifier(context)

                // Check all sites in parallel
                val allResults = sites.map { site ->
                    async { checker.checkSite(site).map { site to it } }
                }.awaitAll().flatten()

                for ((site, result) in allResults) {
                    app.repository.insertResult(result)

                    val shouldNotify = app.retryTracker.record(
                        result.siteId, result.checkType, result.success
                    )
                    if (shouldNotify) {
                        notifier.showFailureNotification(site, result.checkType)
                    } else if (result.success) {
                        notifier.cancelNotification(result.siteId, result.checkType)
                    }
                }

                app.repository.purgeOldResults()

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
