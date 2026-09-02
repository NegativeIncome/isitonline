package com.proinnovation.isitonline.monitor

import android.content.Context
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.notification.AlertNotifier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object MonitorRunner {

    suspend fun runChecks(context: Context) {
        val app = context.applicationContext as IsItOnlineApp
        val sites = app.repository.getEnabledSites()
        if (sites.isEmpty()) return

        val checker = NetworkChecker()
        val notifier = AlertNotifier(context)

        val allResults = coroutineScope {
            sites.map { site ->
                async { checker.checkSite(context, site).map { site to it } }
            }.flatMap { it.await() }
        }

        val resultsBySite = allResults.groupBy { (site, _) -> site }
        for ((site, siteResults) in resultsBySite) {
            val httpsResult = siteResults.find { (_, r) -> r.checkType == "HTTPS" }?.second
            val pingResult  = siteResults.find { (_, r) -> r.checkType == "PING"  }?.second

            siteResults.forEach { (_, result) -> app.repository.insertResult(result) }

            if (httpsResult != null) {
                val shouldNotify = app.retryTracker.record(site.id, "HTTPS", httpsResult.success)
                when {
                    // Notify on a confirmed HTTPS outage unless PING proves the host
                    // is actually reachable. A missing PING result (platform blocked
                    // it) is not proof of reachability, so it must not suppress.
                    shouldNotify && pingResult?.success != true ->
                        notifier.showFailureNotification(site, "HTTPS")
                    httpsResult.success ->
                        notifier.cancelNotification(site.id, "HTTPS")
                }
            }
            notifier.cancelNotification(site.id, "PING")
        }

        app.repository.purgeOldResults()
    }
}
