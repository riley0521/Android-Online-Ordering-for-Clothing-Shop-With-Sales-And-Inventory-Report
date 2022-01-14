package com.teampym.onlineclothingshopapplication.data.network

data class NotificationData(
    val title: String,
    val body: String,
    val orderId: String = "",
    val orderItemId: String = "",
    val postId: String = "",
    val productId: String = ""
)
