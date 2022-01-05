package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.util.ProductType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_wish_list")
@Parcelize
data class WishItem(
    var categoryId: String,
    var name: String,
    var description: String,
    var fileName: String,
    var imageUrl: String,
    var price: Double,
    var productId: String,
    var userId: String,
    @get:Exclude
    var cartId: String,
    @PrimaryKey
    @get:Exclude
    var roomId: String = "",
    var type: String = ProductType.HOODIES.name,
    var dateAdded: Long = 0,
    var dateModified: Long = 0,
    var totalRate: Double = 0.0,
    var numberOfReviews: Long = 0
) : Parcelable {

    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        0.0,
        "",
        "",
        ""
    )

    @get:Exclude
    val avgRate: Double get() = totalRate / numberOfReviews
}
