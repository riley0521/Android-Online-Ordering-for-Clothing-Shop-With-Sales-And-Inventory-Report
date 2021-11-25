package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(
    var id: String,
    var name: String,
    var imageUrl: String,
    var dateAdded: Long = 0,
    var dateModified: Long = 0
) : Parcelable {
    constructor() : this("", "", "")
}
