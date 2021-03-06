package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TermsAndCondition(
    val tc: String,
    val id: String = ""
) : Parcelable {
    constructor() : this(
        ""
    )
}
