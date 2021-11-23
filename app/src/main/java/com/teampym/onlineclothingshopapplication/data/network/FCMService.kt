package com.teampym.onlineclothingshopapplication.data.network

import retrofit2.http.Headers
import retrofit2.http.POST

const val BEARER_KEY = "Bearer AAAA14M9_I4:APA91bF-NXg4ya92mugsrwgrIxZJMUDwXzyEMUBbldDn2kbnKSp1caanxQm6GFFdKKpOZ2O2YVJOH2BUOvupJHj33bPAJ6bItrhIyFQi1-niQLWspcowSKkiupBwYJEQGBdam-RsidhR"

interface FCMService<T> {

    @Headers("Content-Type: application/json", "Authorization: $BEARER_KEY")
    @POST()
    suspend fun notifySingleUser(notificationSingle: NotificationSingle<T>)

    @Headers("Content-Type: application/json", "Authorization: $BEARER_KEY")
    @POST()
    suspend fun notifyToTopics(notificationTopic: NotificationTopic<T>)
}
