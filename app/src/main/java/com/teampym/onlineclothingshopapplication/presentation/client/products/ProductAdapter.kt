package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.EDIT_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.REMOVE_BUTTON
import com.teampym.onlineclothingshopapplication.databinding.ProductItemBinding
import java.lang.NumberFormatException

class ProductAdapter(
    private val listener: OnProductListener,
    private val context: Context
) : PagingDataAdapter<Product, ProductAdapter.ProductViewHolder>(PRODUCT_COMPARATOR) {

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
            binding.apply {
                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null)
                            listener.onItemClicked(item)
                    }
                }

                root.setOnLongClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            val showPopUpMenu = PopupMenu(
                                context,
                                binding.root
                            )

                            showPopUpMenu.menu.add(Menu.NONE, 0, 0, EDIT_BUTTON)
                            showPopUpMenu.menu.add(Menu.NONE, 1, 1, REMOVE_BUTTON)

                            showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                                when (menuItem.itemId) {
                                    0 -> {
                                        listener.onEditClicked(item)
                                        true
                                    }
                                    1 -> {
                                        listener.onDeleteClicked(item)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        }
                    }
                    true
                }

                btnShare.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null)
                            listener.onShareClicked(item)
                    }
                }

                btnAddToCart.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null)
                            listener.onAddToCartClicked(item)
                    }
                }

                btnAddToWishList.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            if (!item.isWishListedByUser) {
                                btnAddToWishList.setImageResource(R.drawable.ic_fav_checked)
                            } else {
                                btnAddToWishList.setImageResource(R.drawable.ic_fav_unchecked)
                            }

                            listener.onAddToWishListClicked(item)
                        }
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

                try {
                    labelRate.isVisible = product.avgRate.toDouble() > 0.0
                    labelRate.text = product.avgRate.toDouble().toString()
                } catch (ex: NumberFormatException) {
                    labelRate.isVisible = false
                }

                if (product.isWishListedByUser) {
                    btnAddToWishList.setImageResource(R.drawable.ic_fav_checked)
                }
            }
        }
    }

    interface OnProductListener {
        fun onItemClicked(product: Product)
        fun onShareClicked(product: Product)
        fun onAddToCartClicked(product: Product)
        fun onAddToWishListClicked(product: Product)
        fun onEditClicked(product: Product)
        fun onDeleteClicked(product: Product)
    }
}
