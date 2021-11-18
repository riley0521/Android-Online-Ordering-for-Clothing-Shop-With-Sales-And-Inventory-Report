package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.Status
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Order(
    val userId: String,
    val totalCost: Double,
    val id: String = "",
    val paymentMethod: String = PaymentMethod.COD.name,
    val isPaid: Boolean = false,
    val status: String = Status.SHIPPING.toString(),
    val orderDate: Long = System.currentTimeMillis(),
    val deliveryInformation: DeliveryInformation = DeliveryInformation(),

    @get:Exclude
    val orderDetails: List<OrderDetail> = emptyList()
) : Parcelable {
    constructor() : this("", 0.0, "")
}
