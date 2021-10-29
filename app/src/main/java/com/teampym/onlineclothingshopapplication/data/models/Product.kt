package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Ignore
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teampym.onlineclothingshopapplication.data.repository.ProductType
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Product(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val flag: String,
    val type: String,
    val inventories: String,
    val productImages: String,
    val reviews: String,
    @get:Exclude
    val inventoryList: List<Inventory>,
    @get:Exclude
    val productImageList: List<ProductImage>,
    @get:Exclude
    val reviewList: List<Review>,
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
        "",
        "",
        "",
        emptyList(),
        emptyList(),
        emptyList()
    )

    @Ignore
    private val gson = Gson()

    @Ignore
    private val iList = object : TypeToken<List<Inventory>>() {}.type

    @Ignore
    private val piList = object : TypeToken<List<ProductImage>>() {}.type

    @Ignore
    private val rList = object : TypeToken<List<Review>>() {}.type

    fun getterInventories(): List<Inventory> = gson.fromJson(inventories, iList)
    fun getterProductImages(): List<ProductImage> = gson.fromJson(productImages, piList)
    fun getterReviews(): List<Review> = gson.fromJson(reviews, rList)

}