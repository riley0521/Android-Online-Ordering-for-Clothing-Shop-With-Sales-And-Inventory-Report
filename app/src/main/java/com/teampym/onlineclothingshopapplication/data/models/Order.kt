package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Order(
    var userId: String,
    var additionalNote: String,
    var deliveryInformation: DeliveryInformation,
    var totalCost: Double,
    var numberOfItems: Long,
    var id: String = "",
    var suggestedShippingFee: Double = 0.0,
    var isUserAgreedToShippingFee: Boolean = false,
    var paymentMethod: String = PaymentMethod.COD.name,
    var status: String = Status.SHIPPING.toString(),
    var dateOrdered: Long = Utils.getTimeInMillisUTC(),
) : Parcelable {

    constructor() : this(
        "",
        "",
        DeliveryInformation(),
        0.0,
        0L
    )

    @get:Exclude
    val totalPaymentWithShippingFee: Double get() = totalCost * suggestedShippingFee

    @get:Exclude
    var orderDetailList: List<OrderDetail> = emptyList()
}
