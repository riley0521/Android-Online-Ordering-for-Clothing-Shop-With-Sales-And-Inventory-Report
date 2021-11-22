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
    var suggestedShippingFee: Double,
    var additionalNote: String,
    var id: String = "",
    var paymentMethod: String = PaymentMethod.COD.name,
    var hasAgreedToShippingFee: Boolean = false,
    var status: String = Status.SHIPPING.toString(),
    var orderDate: Long = System.currentTimeMillis(),
    var deliveryInformation: DeliveryInformation = DeliveryInformation(),

    @get:Exclude
    var orderDetailList: List<OrderDetail> = emptyList()
) : Parcelable {
    constructor() : this("", 0.0, 0.0, "")
}
