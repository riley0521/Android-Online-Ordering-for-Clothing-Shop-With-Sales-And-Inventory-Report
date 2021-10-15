package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Order(
    val id: String,
    val userId: String,
    val fullName: String,
    val address: String,
    val orderDate: Date,
    val totalCost: BigDecimal,
    val status: String,
    val paymentMethod: Boolean
): Parcelable