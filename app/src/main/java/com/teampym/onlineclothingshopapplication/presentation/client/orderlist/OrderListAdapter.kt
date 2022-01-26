package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.CourierType
import com.teampym.onlineclothingshopapplication.data.util.ORDER_COMPLETED_OR_SHOULDERED_BY_ADMIN
import com.teampym.onlineclothingshopapplication.data.util.RECEIVED_ORDER
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.databinding.OrderItemBinding
import java.text.SimpleDateFormat
import java.util.*

class OrderListAdapter(
    private val userType: String,
    private val listener: OnOrderListener,
    private val context: Context
) : PagingDataAdapter<Order, OrderListAdapter.OrderViewHolder>(ORDER_COMPARATOR) {

    companion object {
        private val ORDER_COMPARATOR = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order) =
                oldItem.id == newItem.id &&
                    oldItem.status == newItem.status &&
                    oldItem.receivedByUser == newItem.receivedByUser &&
                    oldItem.recordedToSales == newItem.recordedToSales

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
                labelUsername.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.deliveryInformation.name)
                        }
                    }
                }

                tvUsername.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.deliveryInformation.name)
                        }
                    }
                }

                labelUserID.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.userId)
                        }
                    }
                }

                tvUserID.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.userId)
                        }
                    }
                }

                labelOrderId.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.id)
                        }
                    }
                }

                tvOrderId.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            copyToClipboard(item.id)
                        }
                    }
                }

                btnViewItems.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            listener.onViewItemClicked(item)
                        }
                    }
                }

                btnViewReceipt.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            listener.onViewReceiptClicked(item)
                        }
                    }
                }

                btnCancel.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)

                        if (item != null) {
                            listener.onCancelClicked(item, userType)
                        }
                    }
                }

                btnShipOrder.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)

                        if (item != null) {
                            listener.onShipOrderClicked(item)
                        }
                    }
                }

                btnDeliverOrder.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)

                        val showPopUpMenu = PopupMenu(
                            context,
                            btnDeliverOrder
                        )

                        showPopUpMenu.menu.add(Menu.NONE, 0, 0, "WE DELIVER")
                        showPopUpMenu.menu.add(Menu.NONE, 1, 1, "USE JNT")

                        showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                0 -> {
                                    if (item != null) {
                                        listener.onDeliverOrderClicked(item, CourierType.ADMINS)
                                    }
                                    true
                                }
                                1 -> {
                                    if (item != null) {
                                        listener.onDeliverOrderClicked(item, CourierType.JNT)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }

                        showPopUpMenu.show()
                    }
                }

                btnMarkOrderAsReceived.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        if (item != null) {
                            listener.onMarkOrderAsReceivedClicked(item)
                        }
                    }
                }

                btnActionCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)

                        when (btnActionCompleted.text) {
                            RECEIVED_ORDER -> {
                                if (item != null) {
                                    listener.onReceivedOrderClicked(item)
                                }
                            }
                            ORDER_COMPLETED_OR_SHOULDERED_BY_ADMIN -> {
                                val showPopUpMenu = PopupMenu(
                                    context,
                                    btnActionCompleted
                                )

                                showPopUpMenu.menu.add(Menu.NONE, 0, 0, "ORDER COMPLETED")
                                showPopUpMenu.menu.add(Menu.NONE, 1, 1, "SF SHOULDERED BY ADMIN")

                                showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                                    when (menuItem.itemId) {
                                        0 -> {
                                            if (item != null) {
                                                listener.onCompleteOrderClicked(item, false)
                                            }
                                            true
                                        }
                                        1 -> {
                                            if (item != null) {
                                                listener.onCompleteOrderClicked(item, true)
                                            }
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
        }

        private fun copyToClipboard(text: String) {
            val clipBoard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            val clip = ClipData.newPlainText(text, text)

            clipBoard.setPrimaryClip(clip)

            Toast.makeText(
                context,
                "Copied to clipboard",
                Toast.LENGTH_SHORT
            ).show()
        }

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
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
                        if (userType == UserType.CUSTOMER.name) {
                            btnShipOrder.isVisible = false
                        }

                        btnActionCompleted.isVisible = false
                        btnDeliverOrder.isVisible = false
                    }
                    Status.SHIPPED.name -> {
                        if (userType == UserType.CUSTOMER.name) {
                            btnDeliverOrder.isVisible = false
                        } else if (userType == UserType.ADMIN.name) {
                            btnDeliverOrder.isVisible = true
                        }

                        btnCancel.isVisible = false
                        btnShipOrder.isVisible = false
                        btnActionCompleted.isVisible = false
                    }
                    Status.DELIVERY.name -> {
                        btnCancel.isVisible = false
                        btnShipOrder.isVisible = false
                        btnDeliverOrder.isVisible = false

                        val today = Calendar.getInstance()
                        today.timeInMillis = Utils.getTimeInMillisUTC()

                        if (today.get(Calendar.DAY_OF_YEAR)
                            .minus(calendarDate.get(Calendar.DAY_OF_YEAR)) >= 3
                        ) {
                            btnMarkOrderAsReceived.isVisible = true
                        }

                        if (userType == UserType.CUSTOMER.name && !item.receivedByUser) {
                            btnActionCompleted.text = RECEIVED_ORDER

                            if (item.trackingNumber.isNotBlank()) {
                                btnGoToJntSite.isVisible = true
                                btnGoToJntSite.setOnClickListener {
                                    copyToClipboard(item.trackingNumber)
                                    val jntIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.jtexpress.ph/index/query/gzquery.html")
                                    )
                                    context.startActivity(jntIntent)
                                }
                            }
                        } else {
                            btnActionCompleted.isVisible = false
                        }
                    }
                    Status.COMPLETED.name -> {
                        btnCancel.isVisible = false
                        btnShipOrder.isVisible = false
                        btnDeliverOrder.isVisible = false

                        if (userType == UserType.ADMIN.name) {
                            if (item.receivedByUser && !item.recordedToSales) {
                                btnActionCompleted.text = ORDER_COMPLETED_OR_SHOULDERED_BY_ADMIN
                            } else {
                                btnActionCompleted.isVisible = false
                            }
                        } else {
                            btnActionCompleted.isVisible = false
                            btnViewReceipt.isVisible = true
                        }
                    }
                    Status.CANCELED.name -> {
                        btnCancel.isVisible = false
                        btnShipOrder.isVisible = false
                        btnDeliverOrder.isVisible = false
                        btnActionCompleted.isVisible = false
                    }
                }

                if (userType == UserType.CUSTOMER.name) {
                    labelUsername.isVisible = false
                    tvUsername.isVisible = false

                    labelUserID.isVisible = false
                    tvUserID.isVisible = false

                    labelNumberOfItems.isVisible = false
                    tvNumberOfItems.isVisible = false

                    labelAdditionalNote.isVisible = false
                    tvAdditionalNote.isVisible = false
                }

                tvSuggestedSf.text = context.getString(
                    R.string.placeholder_price,
                    item.shippingFee
                )
                tvOrderId.text = item.id
                tvUserID.text = item.userId
                tvUsername.text = item.deliveryInformation.name
                tvTotalCost.text = context.getString(
                    R.string.placeholder_price,
                    item.totalCost
                )

                tvGrandTotal.text = context.getString(
                    R.string.placeholder_price,
                    item.totalPaymentWithShippingFee
                )

                val completeAddress = "${item.deliveryInformation.streetNumber} " +
                    "${item.deliveryInformation.city}, " +
                    "${item.deliveryInformation.province}, " +
                    "${item.deliveryInformation.region}, " +
                    item.deliveryInformation.postalCode
                tvDeliveryAddress.text = completeAddress

                tvStatus.text = item.status

                val paymentMethodStr = when (item.paymentMethod) {
                    PaymentMethod.COD.name -> {
                        "Cash On Delivery"
                    }
                    PaymentMethod.CREDIT_DEBIT.name -> {
                        "Credit/Debit Card / Paypal"
                    }
                    else -> ""
                }

                tvPaymentMethod.text = paymentMethodStr

                tvPaid.text = if (item.paid) "Yes" else "No"

                tvCourierType.text =
                    if (item.courierType.isNotBlank()) item.courierType else "Not available"
                tvTrackingNumber.text =
                    if (item.trackingNumber.isNotBlank()) item.trackingNumber else "Not available"

                tvNumberOfItems.text = item.numberOfItems.toString()

                tvAdditionalNote.text = item.additionalNote
            }
        }
    }

    interface OnOrderListener {
        fun onViewItemClicked(item: Order)
        fun onViewReceiptClicked(item: Order)
        fun onMarkOrderAsReceivedClicked(item: Order)
        fun onCancelClicked(item: Order, userType: String)
        fun onShipOrderClicked(item: Order)
        fun onDeliverOrderClicked(item: Order, type: CourierType)
        fun onReceivedOrderClicked(item: Order)
        fun onCompleteOrderClicked(item: Order, isSfShoulderedByAdmin: Boolean)
    }
}
