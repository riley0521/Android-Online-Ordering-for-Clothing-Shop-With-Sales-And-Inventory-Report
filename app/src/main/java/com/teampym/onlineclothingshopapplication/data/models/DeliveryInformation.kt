package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DeliveryInformation(
    val id: String,
    val userId: String,
    val contactNo: String,
    val region: String,
    val province: String,
    val city: String,
    val streetNumber: String,
    val postalCode: String
): Parcelable