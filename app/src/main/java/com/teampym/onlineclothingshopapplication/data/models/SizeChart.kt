package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SizeChart(
    val fileName: String,
    val imageUrl: String,
    val id: String = ""
) : Parcelable {

    constructor() : this(
        "",
        ""
    )

}
