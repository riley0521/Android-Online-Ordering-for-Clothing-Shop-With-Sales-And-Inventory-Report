package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FAQModel(
    val question: String,
    val answer: String,
    val id: String = ""
) : Parcelable {

    constructor() : this(
        "",
        ""
    )
}
