package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Inventory(
    val productId: String,
    val size: String,
    val stock: Long,
    val id: String = "",
    val committed: Long = 0,
    val sold: Long = 0,
    val returned: Long = 0,
    val restockLevel: Long = 0
): Parcelable {
    constructor(): this("", "", 0L)
}