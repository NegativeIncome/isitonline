package com.proinnovation.isitonline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

        binding.fabAddSite.setOnClickListener {
            AddSiteDialog().show(supportFragmentManager, "add_site")
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            } else false
        }

        requestNotificationPermission()
        CheckScheduler.scheduleNext(this)
    }

    override fun onSiteAdded(url: String, label: String) {
        viewModel.addSite(url, label)
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
