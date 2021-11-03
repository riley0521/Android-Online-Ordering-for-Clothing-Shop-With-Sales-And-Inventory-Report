package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Ignore
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teampym.onlineclothingshopapplication.data.util.ProductFlag
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    var categoryId: String,
    var name: String,
    var description: String,
    var imageUrl: String,
    var price: Double,
    var type: String,
    var flag: String = ProductFlag.NORMAL.toString(),
    var avgRate: Double = 0.0,
    var id: String = "",
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
        0.0,
        "",
        ""
    )
}