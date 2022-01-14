package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.Product
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class OrderDetail(
    var userId: String,
    var orderId: String,
    var inventoryId: String,
    var size: String,
    var quantity: Long,
    var subTotal: Double,
    var product: Product,
    var id: String = "",
    var dateSold: Long = 0,
    var requestedToReturn: Boolean = false,
    var returned: Boolean = false,
    var canReturn: Boolean = false,
    var canAddReview: Boolean = false,
    var hasAddedReview: Boolean = false
) : Parcelable {

    constructor() : this(
        "",
        "",
        "",
        "",
        0L,
        0.0,
        Product()
    )

    @get:Exclude
    val calculatedPrice: BigDecimal get() = (quantity * product.price).toBigDecimal()
}
