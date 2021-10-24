package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Cart(
    val id: String,
    val userId: String,
    val product: Product,
    val quantity: Long = 1,
    val selectedSizeFromInventory: Inventory,
    val subTotal: BigDecimal
) : Parcelable {
    val calculatedTotalPrice: BigDecimal get() = quantity.times(product.price.toDouble()).toBigDecimal()
}