package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.Status
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Order(
    var userId: String,
    var totalCost: Double,
    var additionalNote: String,
    var id: String = "",
    var suggestedShippingFee: Double = 0.0,
    var isUserAgreedToShippingFee: Boolean = false,
    var paymentMethod: String = PaymentMethod.COD.name,
    var status: String = Status.SHIPPING.toString(),
    var orderDate: Long = System.currentTimeMillis(),
    var deliveryInformation: DeliveryInformation = DeliveryInformation(),

    @get:Exclude
    var orderDetailList: List<OrderDetail> = emptyList()
) : Parcelable {
    constructor() : this("", 0.0, "", "")

    @get:Exclude
    val totalPaymentWithShippingFee: Double get() = totalCost * suggestedShippingFee
}
