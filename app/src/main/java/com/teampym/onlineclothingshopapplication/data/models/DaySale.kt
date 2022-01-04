package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DaySale(
    val totalSale: Double = 0.0,
    val id: String = ""
) : Parcelable
