package com.proinnovation.isitonline.data.repository

import androidx.lifecycle.LiveData
import com.proinnovation.isitonline.data.db.*

class SiteRepository(private val dao: SiteDao) {

    val allSitesLive: LiveData<List<Site>> = dao.getAllSitesLive()
    val latestResultsLive: LiveData<List<LatestCheckResult>> = dao.getLatestResultsLive()

    suspend fun addSite(site: Site): Long = dao.insertSite(site)
    suspend fun updateSite(site: Site) = dao.updateSite(site)
    suspend fun deleteSite(site: Site) = dao.deleteSite(site)
    suspend fun getEnabledSites(): List<Site> = dao.getEnabledSites()
    suspend fun insertResult(result: SiteCheckResult) = dao.insertResult(result)

    fun getResultsForSite(siteId: Long): LiveData<List<SiteCheckResult>> =
        dao.getResultsForSiteLive(siteId)

    suspend fun purgeOldResults() {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        dao.purgeOldResults(cutoff)
    }
}
