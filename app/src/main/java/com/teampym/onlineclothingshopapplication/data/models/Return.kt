package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Return(
    var orderItemId: String,
    var productDetail: String,
    var reason: String,
    @get:Exclude
    var listOfImage: List<UploadedImage> = emptyList()
) : Parcelable {

    constructor() : this(
        "",
        "",
        ""
    )
}
