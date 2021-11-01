package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Ignore
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Product(
    val categoryId: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val flag: String,
    val type: String,
    val id: String = "",
    @get:Exclude
    val inventoryList: List<Inventory> = emptyList(),
    @get:Exclude
    val productImageList: List<ProductImage> = emptyList(),
    @get:Exclude
    val reviewList: List<Review> = emptyList(),
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

    @Ignore
    private val gson = Gson()

    @Ignore
    private val iList = object : TypeToken<List<Inventory>>() {}.type

    @Ignore
    private val piList = object : TypeToken<List<ProductImage>>() {}.type

    @Ignore
    private val rList = object : TypeToken<List<Review>>() {}.type

//    fun getterInventories(): List<Inventory> = gson.fromJson(inventories, iList)
//    fun getterProductImages(): List<ProductImage> = gson.fromJson(productImages, piList)
//    fun getterReviews(): List<Review> = gson.fromJson(reviews, rList)

}