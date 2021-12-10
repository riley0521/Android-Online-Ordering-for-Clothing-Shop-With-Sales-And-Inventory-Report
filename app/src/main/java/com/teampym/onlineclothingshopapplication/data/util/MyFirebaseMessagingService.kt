package com.teampym.onlineclothingshopapplication.data.util

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.room.Product

private const val TAG = "MyFCMService"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.let {
            // Create notification handler here.

            try {
                val order = Gson().fromJson(it["obj"], Order::class.java)
                Log.d(TAG, "onMessageReceived: $order")
            } catch (e: JsonSyntaxException) {
                Log.d(TAG, "onMessageReceived: ${e.message}")
            }

            try {
                val product = Gson().fromJson(it["obj"], Product::class.java)
                Log.d(TAG, "onMessageReceived: $product")
            } catch (e: JsonSyntaxException) {
                Log.d(TAG, "onMessageReceived: ${e.message}")
            }

            try {
                val news = Gson().fromJson(it["obj"], Post::class.java)
                Log.d(TAG, "onMessageReceived: $news")
            } catch (e: JsonSyntaxException) {
                Log.d(TAG, "onMessageReceived: ${e.message}")
            }
        }
    }
}
