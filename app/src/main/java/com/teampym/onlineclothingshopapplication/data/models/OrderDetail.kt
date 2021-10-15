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
    val productId: String,
    val size: String,
    val unitCost: BigDecimal,
    val quantity: Int,
    val totalCost: BigDecimal,
    val dateSold: Date?
): Parcelable