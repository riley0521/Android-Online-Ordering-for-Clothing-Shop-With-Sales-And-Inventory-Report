package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.db.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.ProductType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    var categoryId: String,
    var name: String,
    var description: String,
    var imageUrl: String,
    var price: Double,
    var id: String = "",
    var type: String = ProductType.HOODIES.name,
    var dateAdded: Long = System.currentTimeMillis(),
    @get:Exclude
    var inventoryList: List<Inventory> = emptyList(),
    @get:Exclude
    var productImageList: List<ProductImage> = emptyList(),
    @get:Exclude
    var reviewList: List<Review> = emptyList(),
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        0.0
    )

    @get:Exclude
    val flag: String = SortOrder.BY_NAME.name
}
