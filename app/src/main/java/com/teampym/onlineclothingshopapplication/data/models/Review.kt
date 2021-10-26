package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Review(
    val id: String,
    val productId: String,
    val userId: String,
    val userAvatar: String,
    val username: String,
    val rate: Double,
    val description: String,
    val dateReview: Date
): Parcelable