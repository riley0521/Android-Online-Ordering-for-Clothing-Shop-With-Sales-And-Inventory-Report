package com.teampym.onlineclothingshopapplication.data.util

import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.room.Product
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "MyFCMService"
const val CHANNEL_ID = "midnightmares_ch_here_we_go"
const val ORDER_NOTIFICATION_ID = 777
const val PRODUCT_NOTIFICATION_ID = 778
const val POST_NOTIFICATION_ID = 779

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.let { notificationData ->
            // Create notification handler here.

            showOrderNotification(notificationData)

            showProductNotification(notificationData)

            showPostNotification(notificationData)
        }
    }

    private fun showPostNotification(notificationData: Map<String, String>) =
        try {
            val news = Gson().fromJson(notificationData["obj"], Post::class.java)

            // TODO(Does not have post detail fragment yet)
            // So, no pendingIntent for now.
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_news)
                .setContentTitle(notificationData["title"].toString())
                .setContentText(notificationData["body"].toString())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notificationData["body"].toString())
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(POST_NOTIFICATION_ID, builder.build())
            }

            Log.d(TAG, "onMessageReceived: $news")
        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "onMessageReceived: ${e.message}")
        }

    private fun showProductNotification(notificationData: Map<String, String>) {
        try {
            val product = Gson().fromJson(notificationData["obj"], Product::class.java)

            val pendingIntent = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.productDetailFragment)
                .setArguments(
                    bundleOf(
                        "product" to product,
                        "name" to product.name
                    )
                )
                .createPendingIntent()

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home)
                .setContentTitle(notificationData["title"].toString())
                .setContentText(notificationData["body"].toString())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notificationData["body"].toString())
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(PRODUCT_NOTIFICATION_ID, builder.build())
            }

            Log.d(TAG, "onMessageReceived: $product")
        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "onMessageReceived: ${e.message}")
        }
    }

    private fun showOrderNotification(notificationData: Map<String, String>) {
        try {
            val order = Gson().fromJson(notificationData["obj"], Order::class.java)

            val pendingIntent = NavDeepLinkBuilder(this)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.orderDetailListFragment)
                .setArguments(
                    bundleOf(
                        "title" to "Order ${order.id}",
                        "order" to order
                    )
                )
                .createPendingIntent()

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orders)
                .setContentTitle(notificationData["title"].toString())
                .setContentText(notificationData["body"].toString())
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(notificationData["body"].toString())
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                // notificationId is a unique int for each notification that you must define
                notify(ORDER_NOTIFICATION_ID, builder.build())
            }
        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "onMessageReceived: ${e.message}")
        }
    }
}
