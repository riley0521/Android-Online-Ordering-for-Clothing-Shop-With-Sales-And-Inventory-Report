package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Order(
    var userId: String,
    var additionalNote: String,
    var deliveryInformation: DeliveryInformation,
    var totalCost: Double,
    var shippingFee: Double,
    var numberOfItems: Long,
    var courierType: String,
    var trackingNumber: String,
    var paymentMethod: String,
    var id: String = "",
    var status: String = Status.SHIPPING.toString(),
    var paid: Boolean = false,
    var receivedByUser: Boolean = false,
    var recordedToSales: Boolean = false,
    var dateOrdered: Long = Utils.getTimeInMillisUTC(),
) : Parcelable {

    constructor() : this(
        "",
        "",
        DeliveryInformation(),
        0.0,
        0.0,
        0L,
        "",
        "",
        ""
    )

    @get:Exclude
    val totalPaymentWithShippingFee: Double get() = totalCost + shippingFee

    @get:Exclude
    var orderDetailList: List<OrderDetail> = emptyList()
}
