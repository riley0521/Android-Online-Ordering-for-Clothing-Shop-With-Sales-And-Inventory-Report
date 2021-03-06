package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.OrderDetailItemBinding
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class OrderDetailListAdapter(
    private val listener: OrderDetailListener,
    private val context: Context,
    private val user: UserInformation
) : ListAdapter<OrderDetail, OrderDetailListAdapter.OrderDetailViewHolder>(
    ORDER_DETAIL_COMPARATOR
) {

    companion object {
        private val ORDER_DETAIL_COMPARATOR = object : DiffUtil.ItemCallback<OrderDetail>() {
            override fun areItemsTheSame(oldItem: OrderDetail, newItem: OrderDetail): Boolean {
                return oldItem.id == newItem.id &&
                    oldItem.canReturn == newItem.canReturn &&
                    oldItem.canAddReview == newItem.canAddReview
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
            binding.apply {
                btnReturnItem.setOnClickListener {
                    val pos = absoluteAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = getItem(pos)
                        if (item != null) {
                            listener.onReturnItemClicked(item)
                        }
                    }
                }

                btnAddReview.setOnClickListener {
                    val pos = absoluteAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = getItem(pos)
                        if (item != null) {
                            if (item.canAddReview) {
                                listener.onAddReviewClicked(item)
                            } else {
                                Toast.makeText(
                                    context,
                                    "You cannot add review to this item.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
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

                tvPrice.text = context.getString(
                    R.string.placeholder_price,
                    item.product.price
                )

                txtQuantity.text = item.quantity.toString()

                txtSubtotal.text = context.getString(
                    R.string.placeholder_price,
                    item.subTotal
                )

                if (user.userType == UserType.ADMIN.name) {
                    btnReturnItem.isVisible = false
                }

                if (item.dateSold == 0L) {
                    labelDateSold.isVisible = false
                    txtDateSold.isVisible = false

                    btnReturnItem.isVisible = false
                } else {
                    val calendarDate = Calendar.getInstance()
                    calendarDate.timeInMillis = item.dateSold
                    calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                    val formattedDate =
                        SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

                    txtDateSold.text = formattedDate

                    if (user.userType == UserType.CUSTOMER.name) {
                        btnAddReview.isVisible = !item.hasAddedReview
                        btnReturnItem.isVisible = !item.requestedToReturn
                    }
                }
            }
        }
    }

    interface OrderDetailListener {
        fun onReturnItemClicked(item: OrderDetail): Job
        fun onAddReviewClicked(item: OrderDetail): Job
    }
}
