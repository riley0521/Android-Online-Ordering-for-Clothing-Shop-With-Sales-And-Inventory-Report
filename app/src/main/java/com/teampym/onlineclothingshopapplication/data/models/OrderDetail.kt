package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class OrderDetail(
    val userId: String,
    val orderId: String,
    val inventoryId: String,
    val size: String,
    val quantity: Long,
    val subTotal: Double,
    val id: String = "",
    val dateSold: Long = 0,
    val product: Product = Product(),
    val isExchangeable: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "", 0L, 0.0)
}