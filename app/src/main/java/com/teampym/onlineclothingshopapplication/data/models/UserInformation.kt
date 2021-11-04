package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import java.text.SimpleDateFormat
import java.util.*

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
    @Ignore
    @get:Exclude
    var deliveryInformationList: List<DeliveryInformation> = emptyList(),
    @Ignore
    @get:Exclude
    var notificationTokenList: List<NotificationToken> = emptyList(),
    @Ignore
    @get:Exclude
    var cartList: List<Cart> = emptyList()
) : Parcelable {
    constructor() : this(
        "",
        "",
        ""
    )

    @Ignore
    @IgnoredOnParcel
    private val gson = Gson()

    @Ignore
    @IgnoredOnParcel
    private val dlList = object : TypeToken<List<DeliveryInformation>>() {}.type

    @Ignore
    @IgnoredOnParcel
    private val ntList = object : TypeToken<List<NotificationToken>>() {}.type

    @Ignore
    @IgnoredOnParcel
    private val cList = object : TypeToken<List<Cart>>() {}.type

//    fun getterDeliveryInformation(): List<DeliveryInformation> =
//        gson.fromJson(deliveryInformation, dlList)
//
//    fun getterNotificationTokens(): List<NotificationToken> = gson.fromJson(notificationTokens, ntList)
//    fun getterCart(): List<Cart> = gson.fromJson(cart, cList)
}

fun getDate(date: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    return formatter.format(calendar.time)
}