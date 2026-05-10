package com.proinnovation.isitonline.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proinnovation.isitonline.R
import com.proinnovation.isitonline.data.db.SiteCheckResult
import com.proinnovation.isitonline.databinding.ItemResultBinding
import java.text.SimpleDateFormat
import java.util.*

class ResultsAdapter : ListAdapter<SiteCheckResult, ResultsAdapter.ViewHolder>(DIFF) {

    private val timeOnlyFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())

    private fun formatTimestamp(millis: Long): String {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return if (millis >= todayStart)
            "Today · ${timeOnlyFmt.format(Date(millis))}"
        else
            dateFmt.format(Date(millis))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemResultBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(result: SiteCheckResult) {
            val ctx = b.root.context
            val dotColor = if (result.success)
                ContextCompat.getColor(ctx, R.color.status_green)
            else
                ContextCompat.getColor(ctx, R.color.status_red)
            b.viewStatusDot.backgroundTintList = ColorStateList.valueOf(dotColor)

            b.tvCheckType.text = result.checkType
            b.tvTimestamp.text = formatTimestamp(result.checkedAt)
            b.tvDetails.text = if (result.success) {
                buildString {
                    append("✓")
                    result.responseCode?.let { append("  $it") }
                    result.latencyMs?.let { append("  ${it}ms") }
                    if (length == 1) append("  OK")
                }
            } else {
                "✗  ${result.errorMessage ?: "failed"}"
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SiteCheckResult>() {
            override fun areItemsTheSame(a: SiteCheckResult, b: SiteCheckResult) = a.id == b.id
            override fun areContentsTheSame(a: SiteCheckResult, b: SiteCheckResult) = a == b
        }
    }
}
