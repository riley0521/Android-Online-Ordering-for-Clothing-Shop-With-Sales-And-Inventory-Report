package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.util.Status
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Order(
    val userId: String,
    val totalCost: Double,
    val paymentMethod: String,
    val id: String = "",
    val status: String = Status.SHIPPING.toString(),
    val orderDate: Long = System.currentTimeMillis(),
    val deliveryInformation: DeliveryInformation = DeliveryInformation(),
    val orderDetails: List<OrderDetail> = emptyList()
): Parcelable {
    constructor(): this("", 0.0, "")
}