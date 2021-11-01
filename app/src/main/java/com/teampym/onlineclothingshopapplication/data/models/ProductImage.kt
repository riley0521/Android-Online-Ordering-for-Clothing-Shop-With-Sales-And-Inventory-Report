package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductImage(
    val productId: String,
    val imageUrl: String,
    val id: String = ""
): Parcelable {
    constructor(): this("", "")
}