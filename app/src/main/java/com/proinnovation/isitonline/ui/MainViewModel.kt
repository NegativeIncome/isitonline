package com.proinnovation.isitonline.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.data.db.*
import com.proinnovation.isitonline.monitor.CheckScheduler
import com.proinnovation.isitonline.monitor.MonitorRunner
import com.proinnovation.isitonline.notification.AlertNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IsItOnlineApp
    private val repo = app.repository

    val sitesWithStatus: LiveData<List<SiteWithStatus>> =
        MediatorLiveData<List<SiteWithStatus>>().apply {
            var sites: List<Site> = emptyList()
            var results: List<LatestCheckResult> = emptyList()

            fun combine() {
                val byId = results.groupBy { it.siteId }
                value = sites.map { site ->
                    val latest = byId[site.id] ?: emptyList()
                    SiteWithStatus(
                        site = site,
                        httpsResult = latest.find { it.checkType == "HTTPS" },
                        pingResult = latest.find { it.checkType == "PING" }
                    )
                }
            }

            addSource(repo.allSitesLive) { sites = it; combine() }
            addSource(repo.latestResultsLive) { results = it; combine() }
        }

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    fun checkNow() {
        if (_isRefreshing.value == true) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                withContext(Dispatchers.IO) { MonitorRunner.runChecks(app) }
                if (app.retryTracker.hasActiveRetries()) {
                    CheckScheduler.scheduleRetry(app)
                } else {
                    CheckScheduler.cancelRetry(app)
                    CheckScheduler.scheduleNext(app)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addSite(url: String, rawLabel: String) {
        val label = rawLabel.ifBlank { Uri.parse(url).host ?: url }
        viewModelScope.launch {
            repo.addSite(Site(url = url, label = label))
        }
    }

    fun deleteSite(site: Site) {
        viewModelScope.launch {
            repo.deleteSite(site)
            app.retryTracker.remove(site.id)
            val notifier = AlertNotifier(getApplication())
            notifier.cancelNotification(site.id, "HTTPS")
            notifier.cancelNotification(site.id, "PING")
        }
    }

    fun toggleEnabled(site: Site) {
        viewModelScope.launch {
            repo.updateSite(site.copy(isEnabled = !site.isEnabled))
        }
    }
}
