package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Order(
    val id: String,
    val userId: String,
    val deliveryInformation: DeliveryInformation,
    val orderDate: Date,
    val totalCost: BigDecimal,
    val status: String,
    val paymentMethod: String,
    val orderDetails: List<OrderDetail>? = null
): Parcelable