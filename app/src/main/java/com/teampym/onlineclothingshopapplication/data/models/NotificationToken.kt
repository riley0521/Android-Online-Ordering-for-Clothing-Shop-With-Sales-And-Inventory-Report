package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_tokens")
@Parcelize
data class NotificationToken(
    val userId: String,
    val token: String,
    @PrimaryKey
    val id: String = ""
) : Parcelable {
    constructor(): this("", "")
}