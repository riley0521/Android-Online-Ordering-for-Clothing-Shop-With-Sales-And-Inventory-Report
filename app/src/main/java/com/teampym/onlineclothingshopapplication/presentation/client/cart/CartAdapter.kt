package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.databinding.CartItemBinding

class CartAdapter(
    private val listener: OnItemCartListener
) : ListAdapter<Cart, CartAdapter.CartViewHolder>(CART_COMPARATOR) {

    companion object {
        private val CART_COMPARATOR = object : DiffUtil.ItemCallback<Cart>() {
            override fun areItemsTheSame(oldItem: Cart, newItem: Cart) =
                oldItem.product.id == newItem.product.id

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
                    if(position != RecyclerView.NO_POSITION && btnAdd.isEnabled) {
                        val item = getItem(position)
                        val isMaximum = item.quantity + 1 > item.selectedSizeFromInventory.stock
                        if(isMaximum) {
                            listener.onFailure("You have reached the maximum stocks available for this item.")
                        }
                        else {
                            listener.onAddMinusQuantity(item, item.quantity + 1)
                        }
                    }
                }

                btnRemove.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if(position != RecyclerView.NO_POSITION && btnRemove.isEnabled) {
                        val item = getItem(position)
                        val isMinimum = item.quantity - 1 < 1
                        if(isMinimum) {
                            listener.onFailure("1 is the minimum quantity.")
                        }
                        else {
                            listener.onAddMinusQuantity(item, item.quantity - 1)
                        }
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

                tvName.text = cart.product.name
                tvPrice.text = "$${cart.product.price}"
                tvSize.text = "(${cart.selectedSizeFromInventory.size})"
                tvTotal.text = "$" + "%.2f".format(cart.calculatedTotalPrice)
                tvCount.text = "${cart.quantity}"

                btnAdd.isVisible = cart.selectedSizeFromInventory.stock > cart.quantity
                btnRemove.isVisible = cart.quantity != 1.toLong()
            }
        }

    }

    interface OnItemCartListener {
        fun onAddMinusQuantity(cart: Cart, qty: Long)
        fun onFailure(msg: String)
    }
}