package com.teampym.onlineclothingshopapplication.data.util

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.room.Product
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MyFCMService"

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        remoteMessage.data.let {
            // Create notification handler here.

            try {
                val order = Gson().fromJson(it["obj"], Order::class.java)
                Log.d(TAG, "onMessageReceived: $order")
            } catch (e: JsonSyntaxException) {
                return
            }

            try {
                val product = Gson().fromJson(it["obj"], Product::class.java)
                Log.d(TAG, "onMessageReceived: $product")
            } catch (e: JsonSyntaxException) {
                return
            }
        }
    }
}
