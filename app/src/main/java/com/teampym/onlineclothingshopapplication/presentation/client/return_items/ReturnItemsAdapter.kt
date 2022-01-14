package com.teampym.onlineclothingshopapplication.presentation.client.return_items

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.ReturnItemBinding
import java.text.SimpleDateFormat
import java.util.*

class ReturnItemsAdapter(
    private val listener: ReturnItemsListener,
    private val context: Context,
    private val userType: String
) : PagingDataAdapter<OrderDetail, ReturnItemsAdapter.ReturnItemsViewHolder>(ORDER_DETAIL_COMPARATOR) {

    companion object {
        val ORDER_DETAIL_COMPARATOR = object : DiffUtil.ItemCallback<OrderDetail>() {
            override fun areItemsTheSame(oldItem: OrderDetail, newItem: OrderDetail) =
                oldItem.id == newItem.id &&
                    oldItem.returned == newItem.returned

            override fun areContentsTheSame(oldItem: OrderDetail, newItem: OrderDetail) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReturnItemsViewHolder {
        val binding = ReturnItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReturnItemsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReturnItemsViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            holder.bind(item)
        }
    }

    inner class ReturnItemsViewHolder(private val binding: ReturnItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnViewReason.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        listener.onViewReasonClicked(item)
                    }
                }
            }

            binding.btnConfirmReturnItem.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        listener.onConfirmReturnItemClicked(item)
                    }
                }
            }
        }

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

                tvPrice.text = context.getString(
                    R.string.placeholder_price,
                    item.product.price
                )

                txtQuantity.text = item.quantity.toString()

                txtSubtotal.text = context.getString(
                    R.string.placeholder_price,
                    item.subTotal
                )

                if (userType == UserType.CUSTOMER.name) {
                    btnConfirmReturnItem.isVisible = false
                }

                if (item.dateSold == 0L) {
                    labelDateSold.isVisible = false
                    txtDateSold.isVisible = false
                } else {
                    val calendarDate = Calendar.getInstance()
                    calendarDate.timeInMillis = item.dateSold
                    calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                    val formattedDate =
                        SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

                    txtDateSold.text = formattedDate
                }
            }
        }
    }

    interface ReturnItemsListener {
        fun onViewReasonClicked(item: OrderDetail)
        fun onConfirmReturnItemClicked(item: OrderDetail)
    }
}
