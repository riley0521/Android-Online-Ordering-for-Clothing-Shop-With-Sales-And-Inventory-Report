package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.data.models.Selector
import com.teampym.onlineclothingshopapplication.databinding.SelectRegionProvinceCityItemBinding

class SelectRegionProvinceCityAdapter(
    private val listener: OnSelectorListener
) :
    ListAdapter<Selector, SelectRegionProvinceCityAdapter.SelectRegionProvinceCityViewHolder>(
        SELECTOR_COMPARATOR
    ) {

    companion object {
        private val SELECTOR_COMPARATOR =
            object : DiffUtil.ItemCallback<Selector>() {
                override fun areItemsTheSame(
                    oldItem: Selector,
                    newItem: Selector
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Selector,
                    newItem: Selector
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SelectRegionProvinceCityViewHolder {
        val binding = SelectRegionProvinceCityItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectRegionProvinceCityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectRegionProvinceCityViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class SelectRegionProvinceCityViewHolder(private val binding: SelectRegionProvinceCityItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                tvSelectorName.setOnClickListener {
                    val pos = absoluteAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = getItem(pos)
                        listener.onItemSelected(item)
                    }
                }
            }
        }

        fun bind(item: Selector) {
            binding.apply {
                tvSelectorName.text = item.name
            }
        }
    }

    interface OnSelectorListener {
        fun onItemSelected(selector: Selector)
    }
}
