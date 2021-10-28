package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
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
    val price: BigDecimal,
    val flag: String,
    val type: String,
    val inventories: List<Inventory>? = null,
    val productImages: List<ProductImage>? = null,
    val reviews: List<Review>? = null,
    val isOutOfStock: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "", "", "0".toBigDecimal(), "", ProductType.HOODY.toString())
}