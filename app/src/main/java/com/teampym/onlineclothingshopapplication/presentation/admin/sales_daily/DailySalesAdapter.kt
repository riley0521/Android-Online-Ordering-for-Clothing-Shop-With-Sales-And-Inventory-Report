package com.teampym.onlineclothingshopapplication.presentation.admin.sales_daily

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.DaySale
import com.teampym.onlineclothingshopapplication.databinding.DailySalesItemBinding

class DailySalesAdapter(
    private val context: Context
) : ListAdapter<DaySale, DailySalesAdapter.DailySalesViewHolder>(
    DAY_SALE_COMPARATOR
) {

    companion object {
        val DAY_SALE_COMPARATOR = object : DiffUtil.ItemCallback<DaySale>() {
            override fun areItemsTheSame(oldItem: DaySale, newItem: DaySale) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DaySale, newItem: DaySale) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailySalesViewHolder {
        val binding =
            DailySalesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailySalesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailySalesViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class DailySalesViewHolder(private val binding: DailySalesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DaySale) {
            binding.apply {
                tvDay.text = context.getString(R.string.placeholder_day, item.id.toInt())
                tvTotalSales.text = context.getString(R.string.placeholder_total_sales, item.totalSale)
            }
        }
    }
}
