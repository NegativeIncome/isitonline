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

                // Group by site so PING can serve as a tiebreaker for HTTPS failures.
                // PING-only failures never trigger notifications (ICMP is often blocked).
                val resultsBySite = allResults.groupBy { (site, _) -> site }
                for ((site, siteResults) in resultsBySite) {
                    val httpsResult = siteResults.find { (_, r) -> r.checkType == "HTTPS" }?.second
                    val pingResult  = siteResults.find { (_, r) -> r.checkType == "PING"  }?.second

                    siteResults.forEach { (_, result) -> app.repository.insertResult(result) }

                    if (httpsResult != null) {
                        val shouldNotify = app.retryTracker.record(site.id, "HTTPS", httpsResult.success)
                        when {
                            shouldNotify && pingResult?.success == false ->
                                notifier.showFailureNotification(site, "HTTPS")
                            httpsResult.success ->
                                notifier.cancelNotification(site.id, "HTTPS")
                        }
                    }

                    // Cancel any stale PING-only notifications from before this change
                    notifier.cancelNotification(site.id, "PING")
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
