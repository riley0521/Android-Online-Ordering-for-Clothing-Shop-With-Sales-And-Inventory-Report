package com.teampym.onlineclothingshopapplication.data.network

import com.google.gson.annotations.SerializedName

data class NotificationSingle<T>(
    val data: NotificationData<T>,
    @SerializedName("registration_ids")
    val tokenList: List<String>
)
