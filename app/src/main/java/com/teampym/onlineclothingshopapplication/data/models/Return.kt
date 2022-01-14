package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Return(
    var orderItemId: String,
    var productDetail: String,
    var reason: String,
) : Parcelable {

    constructor() : this(
        "",
        "",
        ""
    )

    @get:Exclude
    var listOfImage: List<UploadedImage> = emptyList()
}
