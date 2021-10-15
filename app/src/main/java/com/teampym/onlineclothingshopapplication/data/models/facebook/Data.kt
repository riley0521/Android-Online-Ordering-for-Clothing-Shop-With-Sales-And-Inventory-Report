package com.teampym.onlineclothingshopapplication.data.models.facebook

import com.google.gson.annotations.SerializedName

data class Data(
    val height: Int,
    @SerializedName("is_silhouette")
    val isSilhouette: Boolean,
    val url: String,
    val width: Int
)