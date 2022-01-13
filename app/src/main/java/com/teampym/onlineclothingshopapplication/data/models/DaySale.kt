package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DaySale(
    var totalSale: Double = 0.0,
    var id: String = ""
) : Parcelable {

    constructor() : this(0.0)

}
