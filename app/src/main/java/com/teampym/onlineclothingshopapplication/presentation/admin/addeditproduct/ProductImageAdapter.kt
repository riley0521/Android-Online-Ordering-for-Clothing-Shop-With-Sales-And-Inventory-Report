package com.teampym.onlineclothingshopapplication.presentation.admin.addeditproduct

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.databinding.ProductImageItemBinding

class ProductImageAdapter(
    private val listener: ProductImageListener
) : ListAdapter<ProductImage, ProductImageAdapter.ProductImageViewHolder>(PRODUCT_IMAGE_COMPARATOR) {

    companion object {
        private val PRODUCT_IMAGE_COMPARATOR = object : DiffUtil.ItemCallback<ProductImage>() {
            override fun areItemsTheSame(oldItem: ProductImage, newItem: ProductImage) =
                oldItem.fileName == newItem.fileName

            override fun areContentsTheSame(oldItem: ProductImage, newItem: ProductImage) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductImageViewHolder {
        val binding = ProductImageItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductImageViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            holder.bind(item)
        }
    }

    inner class ProductImageViewHolder(private val binding: ProductImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.imgRemove.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        listener.onRemoveClicked(item, position)
                    }
                }
            }
        }

        fun bind(item: ProductImage) {
            binding.apply {
                Glide.with(itemView)
                    .load(item.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(binding.imgProductShots)
            }
        }
    }

    interface ProductImageListener {
        fun onRemoveClicked(item: ProductImage, position: Int)
    }
}
