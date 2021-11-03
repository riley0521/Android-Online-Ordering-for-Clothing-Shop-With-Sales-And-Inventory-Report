package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Inventory(
    var productId: String,
    var size: String,
    var stock: Long,
    var id: String = "",
    var committed: Long = 0,
    var sold: Long = 0,
    var returned: Long = 0,
    var restockLevel: Long = 0
): Parcelable {
    constructor(): this("", "", 0L)
}