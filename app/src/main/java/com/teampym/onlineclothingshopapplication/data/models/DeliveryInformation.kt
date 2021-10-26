package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_delivery_info")
@Parcelize
data class DeliveryInformation(
    @PrimaryKey
    val id: String,
    val userId: String,
    val contactNo: String,
    val region: String,
    val province: String,
    val city: String,
    val streetNumber: String,
    val postalCode: String,
    val default: Boolean = false
): Parcelable