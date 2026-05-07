package com.proinnovation.isitonline.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.proinnovation.isitonline.R
import com.proinnovation.isitonline.data.db.LatestCheckResult
import com.proinnovation.isitonline.data.db.Site
import com.proinnovation.isitonline.data.db.SiteWithStatus
import com.proinnovation.isitonline.databinding.ItemSiteBinding
import java.text.SimpleDateFormat
import java.util.*

class SitesAdapter(
    private val onDelete: (Site) -> Unit,
    private val onToggle: (Site) -> Unit
) : RecyclerView.Adapter<SitesAdapter.SiteViewHolder>() {

    private var items: List<SiteWithStatus> = emptyList()

    fun submitList(newItems: List<SiteWithStatus>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(old: Int, new: Int) =
                items[old].site.id == newItems[new].site.id
            override fun areContentsTheSame(old: Int, new: Int) =
                items[old] == newItems[new]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val binding = ItemSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SiteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount() = items.size

    inner class SiteViewHolder(private val b: ItemSiteBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: SiteWithStatus) {
            val ctx = b.root.context

            // Status dot colour
            val dotColor = when (item.overallStatus) {
                SiteWithStatus.Status.OK -> ContextCompat.getColor(ctx, R.color.status_green)
                SiteWithStatus.Status.FAIL -> ContextCompat.getColor(ctx, R.color.status_red)
                SiteWithStatus.Status.UNKNOWN -> ContextCompat.getColor(ctx, R.color.status_grey)
            }
            b.viewStatusDot.backgroundTintList = ColorStateList.valueOf(dotColor)

            b.tvLabel.text = item.site.label
            b.tvUrl.text = item.site.url
            b.tvHttpsStatus.text = formatResult(item.httpsResult, "HTTPS")
            b.tvPingStatus.text = formatResult(item.pingResult, "PING")

            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.isChecked = item.site.isEnabled
            b.switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(item.site) }
            b.btnDelete.setOnClickListener { onDelete(item.site) }
        }

        private fun formatResult(result: LatestCheckResult?, label: String): String {
            if (result == null) return "$label: Never checked"
            return if (result.success) {
                val latency = result.latencyMs?.let { "${it}ms" } ?: ""
                val code = result.responseCode?.let { " $it" } ?: ""
                "$label: ✓$code $latency"
            } else {
                "$label: ✗ ${result.errorMessage ?: "failed"}"
            }
        }
    }
}
