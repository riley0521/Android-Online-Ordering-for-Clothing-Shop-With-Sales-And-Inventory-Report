package com.teampym.onlineclothingshopapplication.presentation.admin.audit_history

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.databinding.HistoryLogItemBinding
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class HistoryLogAdapter(
    private val context: Context
) : PagingDataAdapter<AuditTrail, HistoryLogAdapter.HistoryLogViewHolder>(
    HISTORY_LOG_COMPARATOR
) {

    companion object {
        val HISTORY_LOG_COMPARATOR = object : DiffUtil.ItemCallback<AuditTrail>() {
            override fun areItemsTheSame(oldItem: AuditTrail, newItem: AuditTrail) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: AuditTrail, newItem: AuditTrail) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryLogViewHolder {
        val binding = HistoryLogItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryLogViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class HistoryLogViewHolder(private val binding: HistoryLogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SimpleDateFormat")
        fun bind(historyLog: AuditTrail) {
            binding.apply {
                tvUsername.text =
                    context.getString(R.string.placeholder_username, historyLog.username)
                tvDescription.text =
                    context.getString(R.string.placeholder_description, historyLog.description)
                tvType.text = context.getString(R.string.placeholder_type, historyLog.type)

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = historyLog.dateOfLog
                calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val formattedDate = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a")
                    .format(calendarDate.time)
                tvDate.text = context.getString(R.string.placeholder_date_of_log, formattedDate)
            }
        }
    }
}
