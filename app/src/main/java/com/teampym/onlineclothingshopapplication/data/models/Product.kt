package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.db.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.ProductType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_products")
@Parcelize
data class Product(
    var categoryId: String,
    var name: String,
    var description: String,
    var imageUrl: String,
    var price: Double,
    var productId: String = "",
    @PrimaryKey
    @get:Exclude
    var roomId: String = "",
    @get:Exclude
    var cartId: String = "",
    var type: String = ProductType.HOODIES.name,
    var dateAdded: Long = System.currentTimeMillis(),
    var dateModified: Long = 0,
    @get:Exclude
    @Ignore
    var inventoryList: List<Inventory> = emptyList(),
    @get:Exclude
    @Ignore
    var productImageList: List<ProductImage> = emptyList(),
    @get:Exclude
    @Ignore
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
    @Ignore
    var flag: String = ""
}
