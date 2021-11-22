package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Like(
    var postId: String,
    var userId: String,
    var id: String = ""
) : Parcelable {
    constructor() : this(
        "",
        ""
    )
}
