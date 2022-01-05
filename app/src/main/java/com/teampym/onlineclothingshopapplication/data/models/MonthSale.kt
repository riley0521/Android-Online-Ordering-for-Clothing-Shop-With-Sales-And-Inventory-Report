package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MonthSale(
    val totalSale: Double = 0.0,
    val id: String = ""
) : Parcelable {

    constructor() : this(0.0)

    @get:Exclude
    var listOfDays: List<DaySale> = emptyList()
}
