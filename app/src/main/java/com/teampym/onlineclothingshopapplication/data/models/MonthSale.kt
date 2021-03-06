package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MonthSale(
    var totalSale: Double = 0.0,
    var id: String = "",
    @get:Exclude
    var listOfDays: List<DaySale> = emptyList()
) : Parcelable {

    constructor() : this(0.0)
}
