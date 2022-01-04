package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class YearSale(
    val totalSale: Double = 0.0,
    val id: String = ""
) : Parcelable {

    @get:Exclude
    var listOfMonth: List<MonthSale> = emptyList()
}
