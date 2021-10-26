package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.teampym.onlineclothingshopapplication.data.repository.UserType
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "table_users")
@Parcelize
data class UserInformation(
    @PrimaryKey
    val userId: String,
    val firstName: String,
    val lastName: String,
    @Ignore val deliveryInformation: List<DeliveryInformation>? = null,
    val birthDate: String,
    val avatarUrl: String,
    val userType: String = UserType.CUSTOMER.toString(),
    @Ignore val notificationTokens: List<NotificationToken>? = null,
    @Ignore val cart: List<Cart>? = null,
    val totalOfCart: BigDecimal = "0".toBigDecimal()
) : Parcelable

fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}