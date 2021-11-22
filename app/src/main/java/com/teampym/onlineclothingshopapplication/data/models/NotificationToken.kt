package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_tokens")
@Parcelize
data class NotificationToken(
    val userId: String,
    val token: String,
    @PrimaryKey
    val id: String = "",
    val dateModified: Long = 0,
    val userType: String = UserType.CUSTOMER.name
) : Parcelable {
    constructor() : this("", "")
}
