package com.teampym.onlineclothingshopapplication.data.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService {

    companion object {
        const val BEARER_KEY = "Bearer AAAA14M9_I4:APA91bF-NXg4ya92mugsrwgrIxZJMUDwXzyEMUBbldDn2kbnKSp1caanxQm6GFFdKKpOZ2O2YVJOH2BUOvupJHj33bPAJ6bItrhIyFQi1-niQLWspcowSKkiupBwYJEQGBdam-RsidhR"
    }

    @Headers("Content-Type: application/json", "Authorization: $BEARER_KEY")
    @POST("fcm/send")
    suspend fun notifySingleUser(@Body notificationSingle: NotificationSingle)

    @Headers("Content-Type: application/json", "Authorization: $BEARER_KEY")
    @POST("fcm/send")
    suspend fun notifyToTopics(@Body notificationTopic: NotificationTopic)
}
