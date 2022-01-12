package com.teampym.onlineclothingshopapplication.data.network

import com.google.gson.annotations.SerializedName

data class NotificationSingle(
    val data: NotificationData,
    @SerializedName("registration_ids")
    val tokenList: List<String>
)
