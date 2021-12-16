package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.util.AGREE_TO_SHIPPING_FEE
import com.teampym.onlineclothingshopapplication.data.util.CANCEL_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.CANCEL_OR_SUGGEST
import com.teampym.onlineclothingshopapplication.data.util.ORDER_COMPLETED
import com.teampym.onlineclothingshopapplication.data.util.SUGGEST_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.OrderItemBinding
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class OrderListAdapter(
    private val userType: String,
    private val listener: OnOrderListener,
    private val context: Context
) : PagingDataAdapter<Order, OrderListAdapter.OrderViewHolder>(ORDER_COMPARATOR) {

    companion object {
        private val ORDER_COMPARATOR = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Order, newItem: Order) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = OrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class OrderViewHolder(val binding: OrderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                btnViewItems.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            listener.onItemClicked(item)
                        }
                    }
                }

                btnAction.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)

                        when (btnAction.text) {
                            CANCEL_BUTTON -> {
                                if (item != null) {
                                    listener.onCancelClicked(item, userType)
                                }
                            }
                            CANCEL_OR_SUGGEST -> {
                                if (item != null) {
                                    val showPopUpMenu = PopupMenu(
                                        context,
                                        btnAction
                                    )

                                    showPopUpMenu.menu.add(Menu.NONE, 0, 0, CANCEL_BUTTON)
                                    showPopUpMenu.menu.add(Menu.NONE, 1, 1, SUGGEST_BUTTON)

                                    showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                                        when (menuItem.itemId) {
                                            0 -> {
                                                listener.onCancelClicked(item, userType)
                                                true
                                            }
                                            1 -> {
                                                listener.onSuggestClicked(item)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                }
                            }
                            AGREE_TO_SHIPPING_FEE -> {
                                if (item != null) {
                                    listener.onAgreeToSfClicked(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun bind(item: Order) {
            binding.apply {
                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = item.dateOrdered
                calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val formattedDate =
                    SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

                tvDateOrdered.text = formattedDate

                when (item.status) {
                    Status.SHIPPING.name -> {
                        labelShippingFee.isVisible = false
                        tvSuggestedSf.isVisible = false
                        labelUserAgreedToSf.isVisible = false
                        tvUserAgreedToSf.isVisible = false

                        if (userType == UserType.CUSTOMER.name) {
                            if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                                .minus(calendarDate.get(Calendar.DAY_OF_YEAR)) == 0
                            ) {
                                btnAction.text = CANCEL_BUTTON
                            } else {
                                btnAction.isVisible = false
                            }
                        } else {
                            btnAction.text = CANCEL_OR_SUGGEST
                        }
                    }
                    Status.SHIPPED.name -> {
                        val shippingFeeStr = "$" + item.suggestedShippingFee
                        tvSuggestedSf.text = shippingFeeStr

                        if (userType == UserType.CUSTOMER.name) {
                            btnAction.text = AGREE_TO_SHIPPING_FEE
                        }
                    }
                    Status.DELIVERY.name -> {
                        if (item.isUserAgreedToShippingFee) {
                            tvUserAgreedToSf.text = "Yes"

                            if (userType == UserType.ADMIN.name) {
                                btnAction.text = ORDER_COMPLETED
                            } else {
                                btnAction.isVisible = false
                            }
                        } else {
                            tvUserAgreedToSf.text = "No"
                            btnAction.isVisible = false
                        }
                    }
                    Status.COMPLETED.name -> {
                        // Nothing to do here.
                        btnAction.isVisible = false
                    }
                    Status.RETURNED.name -> {
                        labelShippingFee.isVisible = false
                        tvSuggestedSf.isVisible = false
                        labelUserAgreedToSf.isVisible = false
                        tvUserAgreedToSf.isVisible = false

                        btnAction.isVisible = false
                    }
                    Status.CANCELED.name -> {
                        labelShippingFee.isVisible = false
                        tvSuggestedSf.isVisible = false
                        labelUserAgreedToSf.isVisible = false
                        tvUserAgreedToSf.isVisible = false

                        btnAction.isVisible = false
                    }
                }

                if (userType == UserType.CUSTOMER.name) {
                    labelUsername.isVisible = false
                    tvUsername.isVisible = false

                    labelUserAgreedToSf.isVisible = false
                    tvUserAgreedToSf.isVisible = false

                    labelNumberOfItems.isVisible = false
                    tvNumberOfItems.isVisible = false

                    labelAdditionalNote.isVisible = false
                    tvAdditionalNote.isVisible = false
                }

                val totalCostStr = "$" + item.totalCost

                tvOrderId.text = item.id
                tvUsername.text = item.deliveryInformation.name
                tvTotalCost.text = totalCostStr

                if (item.suggestedShippingFee > 0.0) {
                    val grandTotalStr = "$" + item.totalPaymentWithShippingFee
                    tvGrandTotal.text = grandTotalStr
                } else {
                    labelGrandTotal.isVisible = false
                    tvGrandTotal.isVisible = false
                }

                val completeAddress = "${item.deliveryInformation.streetNumber} " +
                    "${item.deliveryInformation.city}, " +
                    "${item.deliveryInformation.province}, " +
                    "${item.deliveryInformation.region}, " +
                    item.deliveryInformation.postalCode
                tvDeliveryAddress.text = completeAddress

                tvStatus.text = item.status
                tvNumberOfItems.text = item.numberOfItems.toString()

                tvAdditionalNote.text = item.additionalNote
            }
        }
    }

    interface OnOrderListener {
        fun onItemClicked(item: Order)
        fun onCancelClicked(item: Order, userType: String)
        fun onSuggestClicked(item: Order)
        fun onAgreeToSfClicked(item: Order)
    }
}
