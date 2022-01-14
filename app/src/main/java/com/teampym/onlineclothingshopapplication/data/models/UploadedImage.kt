package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UploadedImage(
    var url: String,
    var fileName: String
) : Parcelable {

    constructor() : this(
        "",
        ""
    )
}
