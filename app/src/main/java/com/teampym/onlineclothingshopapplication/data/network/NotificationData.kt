package com.teampym.onlineclothingshopapplication.data.network

data class NotificationData<T>(
    val title: String,
    val body: String,
    val obj: T
)
