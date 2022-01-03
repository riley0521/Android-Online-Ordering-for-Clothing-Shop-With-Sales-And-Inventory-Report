package com.teampym.onlineclothingshopapplication.data.network

import com.teampym.onlineclothingshopapplication.data.models.Post

data class NotificationTopic(
    val data: NotificationData<Post>,
    val to: String = "/topics/news"
)
