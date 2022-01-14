package com.teampym.onlineclothingshopapplication.data.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.models.Review
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_products")
@Parcelize
data class Product(
    var categoryId: String,
    var name: String,
    var description: String,
    var fileName: String,
    var imageUrl: String,
    var price: Double,
    var type: String,
    var productId: String = "",
    @PrimaryKey
    @get:Exclude
    var roomId: String = "",
    @get:Exclude
    var cartId: String = "",
    var dateAdded: Long = 0,
    var dateModified: Long = 0,
    var totalRate: Double = 0.0,
    var numberOfReviews: Long = 0,
    @get:Exclude
    @Ignore
    var isWishListedByUser: Boolean = false
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        0.0,
        ""
    )

    @get:Exclude
    @Ignore
    var flag: String = ""

    @get:Exclude
    val avgRate: Double get() = totalRate / numberOfReviews

    @get:Exclude
    @Ignore
    var fastTrack: Long = 0

    @get:Exclude
    @Ignore
    var inventoryList: List<Inventory> = emptyList()
    @get:Exclude
    @Ignore
    var productImageList: MutableList<ProductImage> = mutableListOf()
    @get:Exclude
    @Ignore
    var reviewList: List<Review> = emptyList()
}
