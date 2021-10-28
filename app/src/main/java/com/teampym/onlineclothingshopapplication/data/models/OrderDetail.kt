package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class OrderDetail(
    val id: String,
    val userId: String,
    val orderId: String,
    val productId: String,
    val inventoryId: String,
    val productName: String,
    val productImage: String,
    val productPrice: BigDecimal,
    val size: String,
    val quantity: Long,
    val subTotal: BigDecimal,
    val dateSold: Date?,
    val isExchangeable: Boolean = dateSold != null
): Parcelable