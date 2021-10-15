package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
data class Inventory(
    val size: String,
    val stock: Long,
    val committed: Long,
    val sold: Long,
    val returned: Long,
    val restockLevel: Long
): Parcelable