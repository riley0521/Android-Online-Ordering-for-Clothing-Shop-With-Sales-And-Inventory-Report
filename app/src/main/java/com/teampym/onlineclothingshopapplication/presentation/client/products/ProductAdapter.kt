package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.databinding.ProductItemBinding

class ProductAdapter(
    private val listener: OnProductListener
) :
    PagingDataAdapter<Product, ProductAdapter.ProductViewHolder>(PRODUCT_COMPARATOR) {

    companion object {
        private val PRODUCT_COMPARATOR = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) =
                oldItem.productId == newItem.productId

            override fun areContentsTheSame(oldItem: Product, newItem: Product) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)

        if (product != null)
            holder.bind(product)
    }

    inner class ProductViewHolder(private val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null)
                        listener.onItemClicked(item)
                }
            }
        }

        fun bind(product: Product) {
            binding.apply {
                Glide.with(itemView)
                    .load(product.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgProduct)

                tvProductName.text = product.name

                val price = "$${product.price.toBigDecimal()}"
                tvPrice.text = price

                if (product.flag.isBlank())
                    tvFlag.isVisible = false
                else
                    tvFlag.text = product.flag

                product.inventoryList.let { inv ->
                    val isOutOfStock = inv.sumOf { it.stock } == 0L
                    labelOutOfStock.isVisible = isOutOfStock

                    val totalSold = inv.sumOf { it.sold }
                    val totalTxt = "Sold $totalSold"
                    labelNumberOfSold.isVisible = totalSold > 0
                    labelNumberOfSold.text = totalTxt
                }

                product.reviewList.let { reviews ->
                    val avgRate: Double = reviews.sumOf { it.rate }.div(reviews.size)
                    labelRate.isVisible = avgRate > 0
                    labelRate.text = avgRate.toString()
                }

                btnShare.setOnClickListener {
                    listener.onShareClicked(product)
                }

                btnAddToCart.setOnClickListener {
                    listener.onAddToCartClicked(product)
                }
            }
        }
    }

    interface OnProductListener {
        fun onItemClicked(product: Product)
        fun onShareClicked(product: Product)
        fun onAddToCartClicked(product: Product)
    }
}
