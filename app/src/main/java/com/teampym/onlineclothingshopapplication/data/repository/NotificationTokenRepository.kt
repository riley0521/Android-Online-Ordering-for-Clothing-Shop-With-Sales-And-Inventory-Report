package com.teampym.onlineclothingshopapplication.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationRepository"

@Singleton
class NotificationTokenRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): List<NotificationToken> {
        return withContext(dispatcher) {
            val notificationTokenList = mutableListOf<NotificationToken>()

            val notificationTokenQuery = userCollectionRef
                .document(userId)
                .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .get()
                .await()

            if (notificationTokenQuery != null && notificationTokenQuery.documents.isNotEmpty()) {
                for (document in notificationTokenQuery.documents) {
                    val notificationToken = document
                        .toObject<NotificationToken>()!!.copy(id = document.id)

                    notificationTokenList.add(notificationToken)
                }
            }
            notificationTokenList
        }
    }

    suspend fun getNewAndSubscribeToTopics(user: UserInformation): NotificationToken? {
        return withContext(dispatcher) {
            // Getting FCM Token
            val result = FirebaseMessaging.getInstance().token.await()
            val createdToken = insert(user.userId, user.userType, result)

            // Subscribe to news or else navigate to admin view
            if (user.userType == UserType.CUSTOMER.name) {
                Firebase.messaging.subscribeToTopic("news").addOnCompleteListener {
                    Log.d(TAG, "getNewAndSubscribeToTopics: SUBSCRIBED!")
                }
            }
            createdToken
        }
    }

    suspend fun insert(
        userId: String,
        userType: String,
        token: String
    ): NotificationToken? {
        return withContext(dispatcher) {
            val createdToken = NotificationToken(
                userId = userId,
                token = token,
                dateAdded = Utils.getTimeInMillisUTC(),
                userType = userType
            )

            val tokenDocument = userCollectionRef
                .document(userId)
                .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .whereEqualTo("token", token)
                .limit(1)
                .get()
                .await()

            if (tokenDocument.documents.size == 1) {
                if (tokenDocument.documents[0]["token"].toString() == token) {
                    return@withContext null
                }
            } else {
                try {
                    val result = userCollectionRef
                        .document(userId)
                        .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                        .add(createdToken)
                        .await()

                    return@withContext createdToken.copy(id = result.id)
                } catch (ex: Exception) {
                    return@withContext null
                }
            }
            null
        }
    }
}
