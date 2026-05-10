package com.proinnovation.isitonline.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SiteDao {

    @Query("SELECT * FROM sites ORDER BY addedAt ASC")
    fun getAllSitesLive(): LiveData<List<Site>>

    @Query("SELECT * FROM sites WHERE isEnabled = 1")
    suspend fun getEnabledSites(): List<Site>

    @Insert
    suspend fun insertSite(site: Site): Long

    @Update
    suspend fun updateSite(site: Site)

    @Delete
    suspend fun deleteSite(site: Site)

    @Insert
    suspend fun insertResult(result: SiteCheckResult)

    @Query("SELECT * FROM LatestCheckResult")
    fun getLatestResultsLive(): LiveData<List<LatestCheckResult>>

    @Query("SELECT * FROM check_results WHERE siteId = :siteId ORDER BY checkedAt DESC LIMIT 200")
    fun getResultsForSiteLive(siteId: Long): LiveData<List<SiteCheckResult>>

    @Query("DELETE FROM check_results WHERE checkedAt < :cutoff")
    suspend fun purgeOldResults(cutoff: Long)
}
