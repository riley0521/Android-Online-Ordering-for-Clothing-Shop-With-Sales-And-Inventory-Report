package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuditTrail(
    val userId: String,
    val description: String,
    val type: String = AuditType.CATEGORY.name,
    val dateOfLog: Long = System.currentTimeMillis(),
    val id: String = ""
) : Parcelable {
    constructor() : this("", "", "")
}
