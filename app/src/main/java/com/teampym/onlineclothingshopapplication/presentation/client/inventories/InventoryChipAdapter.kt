package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.databinding.InventoryItemBinding

class InventoryChipAdapter(
    private val listener: OnItemSizeClickListener
) : ListAdapter<Inventory, InventoryChipAdapter.InventoryChipViewHolder>(
    INVENTORY_COMPARATOR
) {

    companion object {
        private val INVENTORY_COMPARATOR = object : DiffUtil.ItemCallback<Inventory>() {
            override fun areItemsTheSame(oldItem: Inventory, newItem: Inventory) =
                oldItem.size == newItem.size

            override fun areContentsTheSame(oldItem: Inventory, newItem: Inventory) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryChipViewHolder {
        val binding = InventoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InventoryChipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryChipViewHolder, position: Int) {
        val item = getItem(position)

        if(item != null)
            holder.bind(item)
    }

    inner class InventoryChipViewHolder(
        private val binding: InventoryItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                chipSize.setOnClickListener {
                    val position = bindingAdapterPosition
                    if(position != RecyclerView.NO_POSITION) {
                        val inv = getItem(position)
                        listener.onItemClicked(inv)
                    }
                }
            }
        }

        fun bind(inventory: Inventory) {
            binding.apply {
                chipSize.text = inventory.size
            }
        }
    }

    interface OnItemSizeClickListener {
        fun onItemClicked(inventory: Inventory)
    }
}