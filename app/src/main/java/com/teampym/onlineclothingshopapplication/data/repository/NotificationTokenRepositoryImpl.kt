package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.db.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Resource
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationTokenRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val notificationTokenDao: NotificationTokenDao
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): Resource {
        val notificationTokenQuery =
            userCollectionRef.document(userId).collection(NOTIFICATION_TOKENS_SUB_COLLECTION).get()
                .await()
        val notificationTokenList = mutableListOf<NotificationToken>()
        notificationTokenQuery?.let { querySnapshot ->
            for (document in querySnapshot.documents) {
                val notificationToken =
                    document.toObject(NotificationToken::class.java)!!.copy(id = document.id)
                notificationTokenList.add(notificationToken)
                notificationTokenDao.insert(notificationToken)
            }
        }
        return Resource.Success(msg = "Successful", res = notificationTokenList)
    }

    suspend fun upsert(
        userId: String,
        notificationToken: NotificationToken
    ): Resource {
        val isNotificationTokenExistingQuery =
            userCollectionRef.document(userId).collection(NOTIFICATION_TOKENS_SUB_COLLECTION).get()
                .await()
        if (isNotificationTokenExistingQuery != null) {
            for (document in isNotificationTokenExistingQuery.documents) {
                if (document["token"].toString() == notificationToken.token) {

                    val copy = document.toObject(NotificationToken::class.java)!!
                        .copy(id = document.id, token = "Existing")
                    return Resource.Error(msg = "Existing", copy)
                }
            }
        } else {
            val result = userCollectionRef.document(userId).collection(
                NOTIFICATION_TOKENS_SUB_COLLECTION
            ).add(notificationToken).await()
            if (result != null) {
                val res = notificationToken.copy(id = result.id)
                return Resource.Success("Success", res)
            }
        }
        return Resource.Error("Failed", null)
    }

}