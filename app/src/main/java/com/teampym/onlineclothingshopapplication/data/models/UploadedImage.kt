package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UploadedImage(
    val url: String,
    val fileName: String
) : Parcelable
