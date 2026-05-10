package com.proinnovation.isitonline.monitor

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

class RetryTracker(context: Context) {

    private val prefs = context.getSharedPreferences("retry_tracker", Context.MODE_PRIVATE)
    private val counts = ConcurrentHashMap<String, Int>()
    private val MAX_RETRIES = 5

    init {
        prefs.all.forEach { (key, value) ->
            if (value is Int) counts[key] = value
        }
    }

    private fun key(siteId: Long, checkType: String) = "${siteId}_${checkType}"

    /**
     * Records a check outcome. Returns true only on the exact transition to MAX_RETRIES
     * (i.e., the moment a notification should be shown).
     */
    fun record(siteId: Long, checkType: String, success: Boolean): Boolean {
        val k = key(siteId, checkType)
        return if (success) {
            counts[k] = 0
            persist(k, 0)
            false
        } else {
            val current = counts[k] ?: 0
            if (current >= MAX_RETRIES) return false   // already notified; wait for recovery
            val next = current + 1
            counts[k] = next
            persist(k, next)
            next == MAX_RETRIES
        }
    }

    fun getCount(siteId: Long, checkType: String): Int = counts[key(siteId, checkType)] ?: 0

    /** Returns true if any site/type pair is in the retry window (1–4 failures). */
    fun hasActiveRetries(): Boolean = counts.values.any { it in 1 until MAX_RETRIES }

    fun remove(siteId: Long) {
        listOf("HTTPS", "PING").forEach { type ->
            val k = key(siteId, type)
            counts.remove(k)
            prefs.edit().remove(k).apply()
        }
    }

    private fun persist(k: String, count: Int) {
        prefs.edit().putInt(k, count).apply()
    }
}
