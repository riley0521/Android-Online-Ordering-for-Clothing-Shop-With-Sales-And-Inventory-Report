package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.Product
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class OrderDetail(
    val userId: String,
    val orderId: String,
    val inventoryId: String,
    val size: String,
    val quantity: Long,
    val subTotal: Double,
    val id: String = "",
    var dateSold: Long = 0,
    val product: Product = Product(),
    val isExchangeable: Boolean = false
) : Parcelable {
    constructor() : this("", "", "", "", 0L, 0.0)

    @get:Exclude
    val calculatedPrice: BigDecimal get() = (quantity * product.price).toBigDecimal()
}
