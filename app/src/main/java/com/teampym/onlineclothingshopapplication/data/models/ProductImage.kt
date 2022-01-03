package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ProductImage(
    var productId: String,
    var fileName: String,
    var imageUrl: String,
    var id: String = ""
) : Parcelable
