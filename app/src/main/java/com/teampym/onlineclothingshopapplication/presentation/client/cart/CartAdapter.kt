package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.databinding.CartItemBinding

class CartAdapter(
    private val listener: OnItemCartListener,
    private val context: Context
) : ListAdapter<Cart, CartAdapter.CartViewHolder>(CART_COMPARATOR) {

    companion object {
        private val CART_COMPARATOR = object : DiffUtil.ItemCallback<Cart>() {
            override fun areItemsTheSame(oldItem: Cart, newItem: Cart) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Cart, newItem: Cart) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class CartViewHolder(private val binding: CartItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                btnAdd.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION && btnAdd.isEnabled) {
                        val item = getItem(position)
                        val isMaximum = item.quantity + 1 > item.inventory.stock
                        when {
                            isMaximum -> {
                                listener.onFailure("You have reached the maximum stocks available for this item.")
                            }
                            item.quantity + 1 == 100.toLong() -> {
                                listener.onFailure("You can only have 99 pieces per item.")
                            }
                            else -> {
                                listener.onAddQuantity(item.id, position)
                            }
                        }
                    }
                }

                btnRemove.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION && btnRemove.isEnabled) {
                        val item = getItem(position)
                        val isMinimum = item.quantity - 1 < 1
                        if (isMinimum) {
                            listener.onFailure("1 is the minimum quantity.")
                        } else {
                            listener.onRemoveQuantity(item.id, position)
                        }
                    }
                }

                imgDelete.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.onDeleteItemClicked(item.id, position)
                    }
                }
            }
        }

        fun bind(cart: Cart) {
            binding.apply {

                Glide.with(itemView)
                    .load(cart.product.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgCartProduct)

                tvProductName.text = cart.product.name

                tvPrice.text = context.getString(R.string.placeholder_cart_price, cart.product.price)
                tvSize.text = context.getString(R.string.placeholder_cart_size, cart.inventory.size)

                val totalStr = "$" + "%.2f".format(cart.calculatedTotalPrice)
                tvTotal.text = totalStr
                tvCount.text = cart.quantity.toString()

                val isMax = cart.inventory.stock == cart.quantity || cart.quantity == 99.toLong()
                btnAdd.isEnabled = !isMax
                btnRemove.isEnabled = cart.quantity > 1.toLong()

                if (cart.inventory.stock == 0L) {
                    tvOutOfStock.isVisible = true
                    layoutAddRemoveStock.visibility = View.INVISIBLE
                }
            }
        }
    }

    interface OnItemCartListener {
        fun onAddQuantity(cartId: String, pos: Int)
        fun onRemoveQuantity(cartId: String, pos: Int)
        fun onDeleteItemClicked(cartId: String, pos: Int)
        fun onFailure(msg: String)
    }
}
