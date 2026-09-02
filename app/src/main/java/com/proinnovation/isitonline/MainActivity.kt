package com.proinnovation.isitonline

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.proinnovation.isitonline.databinding.ActivityMainBinding
import com.proinnovation.isitonline.monitor.CheckScheduler
import com.proinnovation.isitonline.ui.AddSiteDialog
import com.proinnovation.isitonline.ui.HistoryActivity
import com.proinnovation.isitonline.ui.MainViewModel
import com.proinnovation.isitonline.ui.SettingsActivity
import com.proinnovation.isitonline.ui.SitesAdapter

class MainActivity : AppCompatActivity(), AddSiteDialog.Listener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: SitesAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    private var exactAlarmSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        adapter = SitesAdapter(
            onDelete = { site -> viewModel.deleteSite(site) },
            onToggle = { site -> viewModel.toggleEnabled(site) },
            onItemClick = { item -> HistoryActivity.start(this, item.site.id, item.site.label) }
        )
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.sitesWithStatus.observe(this) { items ->
            adapter.submitList(items)
        }

        viewModel.isRefreshing.observe(this) { refreshing ->
            binding.progressBar.isVisible = refreshing
        }

        binding.fabAddSite.setOnClickListener {
            AddSiteDialog().show(supportFragmentManager, "add_site")
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_check_now -> { viewModel.checkNow(); true }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java)); true
                }
                else -> false
            }
        }

        requestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Re-arm on every resume so that granting the exact-alarm permission and
        // returning to the app immediately upgrades the pending alarm from an
        // inexact (Doze-deferrable) one to an exact one.
        CheckScheduler.scheduleNext(this)
        updateExactAlarmPrompt()
    }

    override fun onSiteAdded(url: String, label: String) {
        viewModel.addSite(url, label)
    }

    /**
     * Android 13+ denies SCHEDULE_EXACT_ALARM by default. Without it, checks fall
     * back to inexact alarms that Doze can defer by up to ~1 hour, so surface a
     * persistent prompt until the user grants it.
     */
    private fun updateExactAlarmPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val am = getSystemService(AlarmManager::class.java)
        if (am.canScheduleExactAlarms()) {
            exactAlarmSnackbar?.dismiss()
            exactAlarmSnackbar = null
            return
        }
        if (exactAlarmSnackbar?.isShownOrQueued == true) return
        exactAlarmSnackbar = Snackbar.make(
            binding.root,
            R.string.exact_alarm_prompt,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.exact_alarm_grant) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            )
        }
        exactAlarmSnackbar?.show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
