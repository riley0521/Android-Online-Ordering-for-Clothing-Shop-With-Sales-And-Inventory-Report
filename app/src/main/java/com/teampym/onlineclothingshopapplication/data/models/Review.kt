package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Review(
    val userId: String,
    val productId: String,
    val userAvatar: String,
    val username: String,
    val rate: Double,
    val description: String,
    val id: String = "",
    val dateReview: Long = System.currentTimeMillis()
): Parcelable {
    constructor(): this("", "", "", "", 0.0, "")
}