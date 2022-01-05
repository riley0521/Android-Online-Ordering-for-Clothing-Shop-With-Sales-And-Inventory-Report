package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.content.Context
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.EDIT_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.REMOVE_BUTTON
import com.teampym.onlineclothingshopapplication.databinding.ProductItemAdminBinding

class ProductAdminAdapter(
    private val context: Context,
    private val listener: OnProductAdminListener
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
        RecyclerView.ViewHolder(binding.root),
        View.OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {

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

                imgMenu.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            val showPopUpMenu = PopupMenu(
                                context,
                                binding.imgMenu
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

                            showPopUpMenu.show()
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

                Log.d("AdminAdapter", product.inventoryList[0].toString())
                tvSize.text = product.inventoryList[0].size
                tvSoldCount.text = product.inventoryList[0].sold.toString()
                tvRemainingStockCount.text = product.inventoryList[0].stock.toString()
            }
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            val edit = menu?.add(Menu.NONE, 1, 1, "Edit")
            edit?.setOnMenuItemClickListener(this)

            val delete = menu?.add(Menu.NONE, 2, 2, "Delete")
            delete?.setOnMenuItemClickListener(this)
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            val position = absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val product = getItem(position)

                if (product != null) {
                    when (item?.itemId) {
                        1 -> {
                            listener.onEditClicked(product)
                            return true
                        }
                        2 -> {
                            AlertDialog.Builder(context)
                                .setTitle("DELETE PRODUCT")
                                .setMessage(
                                    "Are you sure you want to delete this product?\n" +
                                        "All inventories and reviews of this product will also be deleted."
                                )
                                .setPositiveButton("Yes") { _, _ ->
                                    listener.onDeleteClicked(product)
                                }.setNegativeButton("No") { dialog, _ ->
                                    dialog.dismiss()
                                }.show()
                            return true
                        }
                    }
                }
            }
            return false
        }
    }

    interface OnProductAdminListener {
        fun onItemClicked(product: Product)
        fun onEditClicked(product: Product)
        fun onDeleteClicked(product: Product)
    }
}
