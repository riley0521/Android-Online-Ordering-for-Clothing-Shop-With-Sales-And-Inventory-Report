package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuditTrail(
    var username: String,
    var description: String,
    var type: String = AuditType.CATEGORY.name,
    var dateOfLog: Long = Utils.getTimeInMillisUTC(),
    var id: String = ""
) : Parcelable
