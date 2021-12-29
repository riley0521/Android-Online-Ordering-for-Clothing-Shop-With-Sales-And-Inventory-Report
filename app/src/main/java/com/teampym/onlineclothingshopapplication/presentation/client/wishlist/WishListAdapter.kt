package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.databinding.WishItemBinding

class WishListAdapter(
    private val listener: WishListListener
) : ListAdapter<WishItem, WishListAdapter.WishListViewHolder>(
    WISH_ITEM_COMPARATOR
) {

    companion object {
        val WISH_ITEM_COMPARATOR = object : DiffUtil.ItemCallback<WishItem>() {
            override fun areItemsTheSame(oldItem: WishItem, newItem: WishItem) =
                oldItem.productId == newItem.productId

            override fun areContentsTheSame(oldItem: WishItem, newItem: WishItem) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishListViewHolder {
        val binding = WishItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishListViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class WishListViewHolder(private val binding: WishItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)

                    if (item != null) {
                        listener.onItemClicked(item)
                    }
                }
            }

            binding.btnRemoveFromWishList.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)

                    if (item != null) {
                        listener.onRemoveClicked(item, position)
                    }
                }
            }
        }

        fun bind(item: WishItem) {
            binding.apply {
                Glide.with(itemView)
                    .load(item.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgProduct)

                tvProductName.text = item.name

                val priceStr = "$" + "%.2f".format(item.price)
                tvPrice.text = priceStr

                productRating.rating = item.avgRate.toFloat()

                if (!item.avgRate.isNaN()) {
                    labelRate.text = item.avgRate.toString()
                }
            }
        }
    }

    interface WishListListener {
        fun onItemClicked(item: WishItem)
        fun onRemoveClicked(item: WishItem, position: Int)
    }
}
