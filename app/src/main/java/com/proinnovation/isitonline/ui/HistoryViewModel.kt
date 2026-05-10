package com.proinnovation.isitonline.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.data.db.SiteCheckResult

class HistoryViewModel(application: Application, siteId: Long) : AndroidViewModel(application) {

    val results: LiveData<List<SiteCheckResult>> =
        (application as IsItOnlineApp).repository.getResultsForSite(siteId)

    class Factory(
        private val application: Application,
        private val siteId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(application, siteId) as T
    }
}
