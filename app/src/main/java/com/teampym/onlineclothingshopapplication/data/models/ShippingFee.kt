package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShippingFee(
    var metroManila: Int,
    var mindanao: Int,
    var northLuzon: Int,
    var southLuzon: Int,
    var visayas: Int
) : Parcelable {

    constructor() : this(
        0,
        0,
        0,
        0,
        0
    )
}
