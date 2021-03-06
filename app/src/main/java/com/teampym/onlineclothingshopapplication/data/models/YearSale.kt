package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class YearSale(
    var totalSale: Double = 0.0,
    var id: String = ""
) : Parcelable {

    constructor() : this(0.0)

    @get:Exclude
    var listOfMonth: List<MonthSale> = emptyList()
}
