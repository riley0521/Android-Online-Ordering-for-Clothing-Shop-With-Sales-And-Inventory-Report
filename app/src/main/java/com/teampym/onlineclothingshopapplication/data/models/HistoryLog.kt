package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class HistoryLog(
    val id: String,
    val userId: String,
    val description: String,
    val type: String,
    val date: Date
): Parcelable {
    constructor(): this("", "", "", "", Date(0))
}