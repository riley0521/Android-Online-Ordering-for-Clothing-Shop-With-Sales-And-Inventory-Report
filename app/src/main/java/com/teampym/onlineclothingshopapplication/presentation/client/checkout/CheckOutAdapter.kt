package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.databinding.CartItemFinalBinding

class CheckOutAdapter(
    private val context: Context
) : ListAdapter<Cart, CheckOutAdapter.CheckOutViewHolder>(CART_COMPARATOR) {

    companion object {
        private val CART_COMPARATOR = object : DiffUtil.ItemCallback<Cart>() {
            override fun areItemsTheSame(oldItem: Cart, newItem: Cart) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Cart, newItem: Cart) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CheckOutAdapter.CheckOutViewHolder {
        val binding =
            CartItemFinalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CheckOutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckOutAdapter.CheckOutViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class CheckOutViewHolder(private val binding: CartItemFinalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Cart) {
            binding.apply {
                Glide.with(itemView)
                    .load(item.product.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgCartProduct)

                val priceStr = context.getString(
                    R.string.placeholder_price,
                    item.product.price
                )
                val qtyStr = "x${item.quantity} pcs"
                val totalStr = context.getString(
                    R.string.placeholder_total,
                    item.subTotal
                )
                val sizeStr = "Size (${item.inventory.size})"

                tvProductName.text = item.product.name
                tvPrice.text = priceStr
                tvQuantity.text = qtyStr
                tvTotal.text = totalStr
                tvSize.text = sizeStr
            }
        }
    }
}
