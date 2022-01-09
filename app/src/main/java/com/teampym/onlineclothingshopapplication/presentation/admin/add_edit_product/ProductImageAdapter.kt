package com.teampym.onlineclothingshopapplication.presentation.admin.add_edit_product

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.databinding.ProductImageItemBinding

class ProductImageAdapter(
    private val listener: ProductImageListener,
    private val additionalImageList: List<Any>
) : RecyclerView.Adapter<ProductImageAdapter.ProductImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductImageViewHolder {
        val binding = ProductImageItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductImageViewHolder, position: Int) {
        val item = additionalImageList[position]

        holder.bind(item)
    }

    override fun getItemCount() = additionalImageList.size

    inner class ProductImageViewHolder(private val binding: ProductImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.imgRemove.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRemoveClicked(position)
                }
            }
        }

        fun bind(item: Any) {
            when (item) {
                is Uri -> {
                    Glide.with(itemView)
                        .load(item)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_food)
                        .into(binding.imgProductShots)
                }
                is ProductImage -> {
                    Glide.with(itemView)
                        .load(item.imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_food)
                        .into(binding.imgProductShots)
                }
            }
        }
    }

    interface ProductImageListener {
        fun onRemoveClicked(position: Int)
    }
}
