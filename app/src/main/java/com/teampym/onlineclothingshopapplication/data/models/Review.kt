package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Review(
    var userId: String,
    var productId: String,
    var userAvatar: String,
    var username: String,
    var rate: Double,
    var description: String,
    var productSize: String,
    var id: String = "",
    var dateReview: Long = Utils.getTimeInMillisUTC()
) : Parcelable {

    constructor() : this(
        "",
        "",
        "",
        "",
        0.0,
        "",
        ""
    )

}
