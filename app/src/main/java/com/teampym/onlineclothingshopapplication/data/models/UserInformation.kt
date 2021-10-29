package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.firebase.firestore.Exclude
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    var birthDate: String,
    var avatarUrl: String,
    var userType: String,
    var totalOfCart: Double,
    var deliveryInformation: String,
    var notificationTokens: String,
    var cart: String,
    @Ignore
    @get:Exclude
    var deliveryInformationList: List<DeliveryInformation>,
    @Ignore
    @get:Exclude
    var notificationTokenList: List<NotificationToken>,
    @Ignore
    @get:Exclude
    var cartList: List<Cart>
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        0.0,
        "",
        "",
        "",
        emptyList (),
        emptyList(),
        emptyList()
    )

    @Ignore
    private val gson = Gson()

    @Ignore
    private val dlList = object : TypeToken<List<DeliveryInformation>>() {}.type

    @Ignore
    private val ntList = object : TypeToken<List<NotificationToken>>() {}.type

    @Ignore
    private val cList = object : TypeToken<List<Cart>>() {}.type

    fun getterDeliveryInformation(): List<DeliveryInformation> =
        gson.fromJson(deliveryInformation, dlList)

    fun getterNotificationTokens(): List<NotificationToken> = gson.fromJson(notificationTokens, ntList)
    fun getterCart(): List<Cart> = gson.fromJson(cart, cList)
}

fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    val formatter = SimpleDateFormat(format, locale)
    return formatter.format(this)
}