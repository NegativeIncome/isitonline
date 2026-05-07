package com.proinnovation.isitonline.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class Site(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val label: String,
    val isEnabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
