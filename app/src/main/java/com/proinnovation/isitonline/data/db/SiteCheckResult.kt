package com.proinnovation.isitonline.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_results",
    foreignKeys = [ForeignKey(
        entity = Site::class,
        parentColumns = ["id"],
        childColumns = ["siteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SiteCheckResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val siteId: Long,
    val checkType: String,      // "HTTPS" or "PING"
    val checkedAt: Long,
    val success: Boolean,
    val responseCode: Int?,     // HTTP status code; null for ping
    val latencyMs: Long?,
    val errorMessage: String?
)
