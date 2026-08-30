package com.proinnovation.isitonline.data.db

import androidx.room.DatabaseView

@DatabaseView(
    """
    SELECT cr.* FROM check_results cr
    INNER JOIN (
        SELECT siteId, checkType, MAX(checkedAt) AS maxAt
        FROM check_results
        GROUP BY siteId, checkType
    ) latest
    ON cr.siteId = latest.siteId
    AND cr.checkType = latest.checkType
    AND cr.checkedAt = latest.maxAt
    """
)
data class LatestCheckResult(
    val id: Long,
    val siteId: Long,
    val checkType: String,
    val checkedAt: Long,
    val success: Boolean,
    val responseCode: Int?,
    val latencyMs: Long?,
    val errorMessage: String?,
    val diagnostics: String?
)
