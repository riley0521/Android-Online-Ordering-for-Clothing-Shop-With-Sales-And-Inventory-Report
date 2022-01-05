package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Category(
    var name: String,
    var fileName: String,
    var imageUrl: String,
    var id: String = "",
    var dateAdded: Long = 0,
    var dateModified: Long = 0
) : Parcelable {

    constructor() : this(
        "",
        "",
        ""
    )

}
