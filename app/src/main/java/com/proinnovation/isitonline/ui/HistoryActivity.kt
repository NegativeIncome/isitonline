package com.proinnovation.isitonline.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.proinnovation.isitonline.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SITE_ID = "site_id"
        private const val EXTRA_SITE_LABEL = "site_label"

        fun start(context: Context, siteId: Long, siteLabel: String) {
            context.startActivity(
                Intent(context, HistoryActivity::class.java)
                    .putExtra(EXTRA_SITE_ID, siteId)
                    .putExtra(EXTRA_SITE_LABEL, siteLabel)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val siteId = intent.getLongExtra(EXTRA_SITE_ID, -1)
        val siteLabel = intent.getStringExtra(EXTRA_SITE_LABEL) ?: "History"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = siteLabel
        }

        val factory = HistoryViewModel.Factory(application, siteId)
        val viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]

        val adapter = ResultsAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )
        binding.recyclerView.adapter = adapter

        viewModel.results.observe(this) { results ->
            adapter.submitList(results)
            binding.tvEmpty.isVisible = results.isEmpty()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
