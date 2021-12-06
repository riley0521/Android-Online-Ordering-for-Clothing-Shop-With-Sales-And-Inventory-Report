package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.NotificationData
import com.teampym.onlineclothingshopapplication.data.network.NotificationSingle
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTokenRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    @Inject
    lateinit var service: FCMService<Any>

    suspend fun getAll(userId: String): List<NotificationToken> {
        return withContext(dispatcher) {
            val notificationTokenList = mutableListOf<NotificationToken>()

            val notificationTokenQuery = userCollectionRef
                .document(userId)
                .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .get()
                .await()

            if (notificationTokenQuery.documents.isNotEmpty()) {
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
            var createdToken: NotificationToken? = null

            // Getting FCM Token
            val result = FirebaseMessaging.getInstance().token.await()
            createdToken = insert(user.userId, user.userType, result)

            // Subscribe to news or else navigate to admin view
            if (user.userType == UserType.CUSTOMER.name) {
                Firebase.messaging.subscribeToTopic("news").await()
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
            var createdToken: NotificationToken? = NotificationToken(
                userId = userId,
                token = token,
                dateModified = System.currentTimeMillis(),
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
                    createdToken = null
                }
            } else {
                createdToken?.let { nf ->
                    val result = userCollectionRef
                        .document(userId)
                        .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                        .add(nf)
                        .await()
                    if (result != null) {
                        createdToken?.id = result.id
                    } else {
                        createdToken = null
                    }
                }
            }
            createdToken
        }
    }

    suspend fun notifyCustomer(
        obj: Any?,
        userId: String,
        title: String,
        body: String
    ): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true

            var objNotNull = Any()
            if (obj != null) {
                objNotNull = obj
            }

            val data = NotificationData(
                title = title,
                body = body,
                obj = objNotNull
            )

            val notificationTokenList = getAll(userId)

            if(notificationTokenList.isNotEmpty()) {
                val tokenList: List<String> = notificationTokenList.map {
                    it.token
                }

                val notificationSingle = NotificationSingle(
                    data = data,
                    tokenList = tokenList
                )

                service.notifySingleUser(notificationSingle)
            } else {
                isCompleted = false
            }
            isCompleted
        }
    }

    suspend fun notifyAllAdmins(
        obj: Any?,
        title: String,
        body: String
    ): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            var objNotNull = Any()
            obj?.let {
                objNotNull = it
            }

            val res = db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .whereEqualTo("userType", UserType.ADMIN.name)
                .get()
                .await()

            if (res.documents.isNotEmpty()) {
                val tokenList = mutableListOf<String>()
                for (doc in res.documents) {
                    val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                    tokenList.add(token.token)
                }

                val data = NotificationData(
                    title = title,
                    body = body,
                    obj = objNotNull
                )

                val notificationSingle = NotificationSingle(
                    data = data,
                    tokenList = tokenList
                )

                service.notifySingleUser(notificationSingle)
            } else {
                isCompleted = false
            }
            isCompleted
        }
    }
}
