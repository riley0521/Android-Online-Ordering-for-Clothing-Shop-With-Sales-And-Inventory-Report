package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comment(
    var postId: String,
    var description: String,
    var commentBy: String,
    var id: String = "",
    var dateCommented: Long = System.currentTimeMillis()
) : Parcelable {
    constructor() : this(
        "",
        "",
        ""
    )
}
