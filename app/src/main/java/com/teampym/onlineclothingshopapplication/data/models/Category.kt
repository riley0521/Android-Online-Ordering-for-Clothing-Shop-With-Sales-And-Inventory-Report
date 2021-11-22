package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(
    var id: String,
    var name: String,
    var imageUrl: String,
) : Parcelable {
    constructor() : this("", "", "")
}
