package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductImage(
    var productId: String,
    var imageUrl: String,
    var id: String = ""
): Parcelable {
    constructor(): this("", "")
}