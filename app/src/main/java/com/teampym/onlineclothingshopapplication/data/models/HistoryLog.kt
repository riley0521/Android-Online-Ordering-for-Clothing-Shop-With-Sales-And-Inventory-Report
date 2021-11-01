package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class HistoryLog(
    val userId: String,
    val description: String,
    val type: String,
    val date: Long = System.currentTimeMillis(),
    val id: String = ""
): Parcelable {
    constructor(): this("", "", "")
}