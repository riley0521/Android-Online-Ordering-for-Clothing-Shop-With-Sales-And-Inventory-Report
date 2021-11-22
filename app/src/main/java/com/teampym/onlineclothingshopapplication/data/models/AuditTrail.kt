package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuditTrail(
    var userId: String,
    var description: String,
    var type: String = AuditType.CATEGORY.name,
    var dateOfLog: Long = System.currentTimeMillis(),
    var id: String = ""
) : Parcelable {
    constructor() : this("", "", "")
}
