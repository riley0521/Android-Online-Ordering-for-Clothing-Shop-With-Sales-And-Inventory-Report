package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class OrderDetail(
    val id: String,
    val orderId: String,
    val product: Product,
    val size: String,
    val price: BigDecimal,
    val quantity: Long,
    val subTotal: BigDecimal,
    val dateSold: Date?
): Parcelable