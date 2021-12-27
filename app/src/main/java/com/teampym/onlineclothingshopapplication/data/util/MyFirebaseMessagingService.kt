package com.teampym.onlineclothingshopapplication.data.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MyFCMService"
const val CHANNEL_ID = "my_channel"
const val ORDER_NOTIFICATION_ID = 777
const val PRODUCT_NOTIFICATION_ID = 778
const val POST_NOTIFICATION_ID = 779

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    init {
        createNotificationChannel()
    }

    @Inject
    lateinit var userInformationDao: UserInformationDao

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

            var userInfo: UserInformation? = null

            CoroutineScope(Dispatchers.IO).launch {
                userInfo = async { userInformationDao.getCurrentUser(order.userId) }.await()
            }.invokeOnCompletion {
                // Create an explicit intent for an Activity in your app
                userInfo?.let { u ->
                    val pendingIntent = NavDeepLinkBuilder(this)
                        .setGraph(R.navigation.nav_graph)
                        .setDestination(R.id.orderDetailListFragment)
                        .setArguments(
                            bundleOf(
                                "title" to "Order ${order.id}",
                                "order" to order,
                                "userInfo" to u
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
                }
            }
        } catch (e: JsonSyntaxException) {
            Log.d(TAG, "onMessageReceived: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
