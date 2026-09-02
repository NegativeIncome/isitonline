package com.proinnovation.isitonline.data.db

data class SiteWithStatus(
    val site: Site,
    val httpsResult: LatestCheckResult?,
    val pingResult: LatestCheckResult?
) {
    enum class Status { OK, FAIL, UNKNOWN }

    // HTTPS is the authoritative signal; PING is only consulted when there is no
    // HTTPS result yet, since on Android 10+ ping is often unavailable and its
    // failures (or stale rows from before it was dropped) don't mean the site is down.
    val overallStatus: Status
        get() = when {
            httpsResult != null -> if (httpsResult.success) Status.OK else Status.FAIL
            pingResult != null -> if (pingResult.success) Status.OK else Status.FAIL
            else -> Status.UNKNOWN
        }
}
