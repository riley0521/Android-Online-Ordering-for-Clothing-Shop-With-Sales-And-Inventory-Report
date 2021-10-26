package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_notification_token")
@Parcelize
data class NotificationToken(
    @PrimaryKey
    val id: String,
    val userId: String,
    val token: String
) : Parcelable