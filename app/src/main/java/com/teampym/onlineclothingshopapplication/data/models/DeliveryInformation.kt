package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "table_delivery_info")
@Parcelize
data class DeliveryInformation(
    var name: String,
    var contactNo: String,
    var region: String,
    var province: String,
    var city: String,
    var streetNumber: String,
    var postalCode: String,
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var isPrimary: Boolean = false
) : Parcelable {
    constructor() : this("", "", "", "", "", "", "")
}
