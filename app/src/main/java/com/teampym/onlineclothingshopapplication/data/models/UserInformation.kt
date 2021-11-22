package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

@Entity(tableName = "table_users")
@Parcelize
data class UserInformation(
    var firstName: String,
    var lastName: String,
    var birthDate: String,
    var avatarUrl: String? = null,
    @PrimaryKey
    var userId: String = "",
    var userType: String = UserType.CUSTOMER.toString(),
    var dateModified: Long = 0,
    @Ignore
    @get:Exclude
    var deliveryInformationList: List<DeliveryInformation> = emptyList(),
    @Ignore
    @get:Exclude
    var notificationTokenList: List<NotificationToken> = emptyList(),
    @Ignore
    @get:Exclude
    var wishList: List<WishItem> = emptyList(),
    @Ignore
    @get:Exclude
    var cartList: List<Cart> = emptyList()
) : Parcelable {
    constructor() : this(
        "",
        "",
        ""
    )
}

fun getDate(date: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    return formatter.format(calendar.time)
}
