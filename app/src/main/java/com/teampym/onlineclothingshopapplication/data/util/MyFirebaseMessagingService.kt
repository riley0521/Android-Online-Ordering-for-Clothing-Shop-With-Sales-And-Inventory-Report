package com.teampym.onlineclothingshopapplication.data.util

import android.app.PendingIntent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teampym.onlineclothingshopapplication.R

private const val TAG = "MyFCMService"
const val CHANNEL_ID = "midnightmares_ch_here_we_go"
private const val APP_NOTIFICATION_ID = 777

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.let { notificationData ->
            // Create notification handler here.

            Log.d(TAG, "onMessageReceived: $notificationData")

            var pendingIntent: PendingIntent? = null

            when {
                notificationData["orderId"]?.isNotBlank()!! -> {
                    val orderId = notificationData["orderId"]

                    pendingIntent = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph)
                        .setDestination(R.id.orderDetailListFragment)
                        .setArguments(
                            bundleOf(
                                "title" to "Order $orderId",
                                "orderId" to orderId
                            )
                        )
                        .createPendingIntent()
                }
                notificationData["postId"]?.isNotBlank()!! -> {
                }
                notificationData["productId"]?.isNotBlank()!! -> {
                    val productId = notificationData["productId"]

                    pendingIntent = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph)
                        .setDestination(R.id.productDetailFragment)
                        .setArguments(
                            bundleOf(
                                "productId" to productId
                            )
                        )
                        .createPendingIntent()
                }
            }

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
                notify(APP_NOTIFICATION_ID, builder.build())
            }
        }
    }
}
