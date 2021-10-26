package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Entity(tableName = "table_cart")
@Parcelize
data class Cart(
    @PrimaryKey
    val id: String,
    val userId: String,
    val product: Product,
    val quantity: Long = 1,
    val selectedSizeFromInventory: Inventory,
    val subTotal: BigDecimal
) : Parcelable {
    val calculatedTotalPrice: BigDecimal get() = quantity.times(product.price.toDouble()).toBigDecimal()
}