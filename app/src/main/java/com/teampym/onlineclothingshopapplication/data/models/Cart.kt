package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Cart(
    var product: Product,
    var quantity: Long = 1,
    var selectedSizeFromInventory: Inventory,
    var subTotal: Double
) : Parcelable {
    val calculatedTotalPrice: Double get() = quantity.times(product.price.toDouble())
}