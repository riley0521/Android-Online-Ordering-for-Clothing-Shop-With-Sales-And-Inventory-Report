package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.databinding.OrderDetailItemBinding
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class OrderDetailListAdapter(
    private val listener: OrderDetailListener
) : ListAdapter<OrderDetail, OrderDetailListAdapter.OrderDetailViewHolder>(
    ORDER_DETAIL_COMPARATOR
) {

    companion object {
        private val ORDER_DETAIL_COMPARATOR = object : DiffUtil.ItemCallback<OrderDetail>() {
            override fun areItemsTheSame(oldItem: OrderDetail, newItem: OrderDetail): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: OrderDetail, newItem: OrderDetail): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailViewHolder {
        val binding =
            OrderDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderDetailViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            holder.bind(item)
        }
    }

    inner class OrderDetailViewHolder(private val binding: OrderDetailItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnExchangeItem.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = getItem(pos)
                    if (item != null) {
                        listener.onExchangeItemClicked(item)
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        fun bind(item: OrderDetail) {
            binding.apply {
                Glide.with(itemView)
                    .load(item.product.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgProduct)

                tvProductName.text = item.product.name
                tvSize.text = item.size

                val priceStr = "$" + item.product.price
                tvPrice.text = priceStr

                txtQuantity.text = item.quantity.toString()

                val subTotalStr = "$" + item.calculatedPrice
                txtSubtotal.text = subTotalStr

                if (item.dateSold == 0L) {
                    labelDateSold.isVisible = false
                    txtDateSold.isVisible = false

                    labelIsExchangeable.isVisible = false
                    txtIsExchangeable.isVisible = false

                    btnExchangeItem.isVisible = false
                } else {
                    val calendarDate = Calendar.getInstance()
                    calendarDate.timeInMillis = item.dateSold
                    val formattedDate =
                        SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

                    txtDateSold.text = formattedDate
                    txtIsExchangeable.text = "Yes"
                }
            }
        }
    }

    interface OrderDetailListener {
        fun onExchangeItemClicked(item: OrderDetail)
    }
}
