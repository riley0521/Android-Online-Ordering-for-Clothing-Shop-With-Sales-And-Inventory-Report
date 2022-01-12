package com.teampym.onlineclothingshopapplication.data.network

data class NotificationTopic(
    val data: NotificationData,
    val to: String = "/topics/news"
)
