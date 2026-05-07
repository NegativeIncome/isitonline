package com.proinnovation.isitonline.data.db

data class SiteWithStatus(
    val site: Site,
    val httpsResult: LatestCheckResult?,
    val pingResult: LatestCheckResult?
) {
    enum class Status { OK, FAIL, UNKNOWN }

    val overallStatus: Status
        get() = when {
            httpsResult == null && pingResult == null -> Status.UNKNOWN
            httpsResult?.success == false || pingResult?.success == false -> Status.FAIL
            else -> Status.OK
        }
}
