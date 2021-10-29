package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.teampym.onlineclothingshopapplication.data.repository.UserType
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "table_users")
@Parcelize
data class UserInformation(
    @PrimaryKey
    var userId: String = "",
    var firstName: String,
    var lastName: String,
    @Ignore var deliveryInformation: List<DeliveryInformation>? = null,
    var birthDate: String,
    var avatarUrl: String,
    var userType: String = UserType.CUSTOMER.toString(),
    @Ignore var notificationTokens: List<NotificationToken>? = null,
    @Ignore var cart: List<Cart>? = null,
    var totalOfCart: Double = 0.0
) : Parcelable {
    constructor() : this("", "", "", null, "", "", UserType.CUSTOMER.toString(), null, null, 0.0)
}

fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}