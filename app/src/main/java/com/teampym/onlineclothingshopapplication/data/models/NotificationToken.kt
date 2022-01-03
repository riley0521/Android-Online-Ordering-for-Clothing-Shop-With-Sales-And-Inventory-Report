package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_tokens")
@Parcelize
data class NotificationToken(
    var userId: String,
    var token: String,
    @PrimaryKey
    var id: String = "",
    var dateModified: Long = 0,
    var userType: String = UserType.CUSTOMER.name
) : Parcelable
