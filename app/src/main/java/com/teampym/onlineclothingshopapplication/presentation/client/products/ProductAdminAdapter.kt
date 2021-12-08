package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.databinding.ProductItemAdminBinding

class ProductAdminAdapter(
    private val listener: OnProductAdapterListener
) : PagingDataAdapter<Product, ProductAdminAdapter.ProductAdminViewHolder>(PRODUCT_COMPARATOR) {

    companion object {
        private val PRODUCT_COMPARATOR = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) =
                oldItem.productId == newItem.productId

            override fun areContentsTheSame(oldItem: Product, newItem: Product) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAdminViewHolder {
        val binding =
            ProductItemAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductAdminViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductAdminViewHolder, position: Int) {
        val product = getItem(position)

        if (product != null)
            holder.bind(product)
    }

    inner class ProductAdminViewHolder(private val binding: ProductItemAdminBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null)
                            listener.onItemClicked(item)
                    }
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

                Log.d("AdminAdapter", product.inventoryList[0].toString())
                tvSize.text = product.inventoryList[0].size
                tvSoldCount.text = product.inventoryList[0].sold.toString()
                tvRemainingStockCount.text = product.inventoryList[0].stock.toString()
            }
        }
    }

    interface OnProductAdapterListener {
        fun onItemClicked(product: Product)
    }
}
