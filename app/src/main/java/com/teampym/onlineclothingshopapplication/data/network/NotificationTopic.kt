package com.teampym.onlineclothingshopapplication.data.network

data class NotificationTopic<T>(
    val data: NotificationData<T>,
    val to: String = "/topics/news"
)
