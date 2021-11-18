package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.room.Cart
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Checkout(
    val userId: String,
    val cart: List<Cart> = emptyList(),
    val totalCost: BigDecimal
): Parcelable
