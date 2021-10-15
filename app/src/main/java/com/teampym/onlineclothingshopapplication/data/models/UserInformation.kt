package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.teampym.onlineclothingshopapplication.data.repository.UserType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UserInformation(
    val id: String = "",
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val avatarUrl: String = "",
    val contactInformation: String,
    val userType: String = UserType.CUSTOMER.toString(),
    val notificationTokens: List<NotificationToken> = emptyList(),
    val cart: List<Cart> = emptyList()
) : Parcelable {
    constructor() : this("failed", "failed", "failed", "failed", "failed", "failed", "failed",)
}