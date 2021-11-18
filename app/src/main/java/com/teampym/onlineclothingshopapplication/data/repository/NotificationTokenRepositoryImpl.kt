package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
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

    suspend fun getAll(userId: String): List<NotificationToken> {
        val notificationTokenQuery = userCollectionRef
            .document(userId)
            .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
            .get()
            .await()

        val notificationTokenList = mutableListOf<NotificationToken>()
        if (notificationTokenQuery.documents.isNotEmpty()) {
            for (document in notificationTokenQuery.documents) {
                val notificationToken = document
                    .toObject(NotificationToken::class.java)!!.copy(id = document.id)

                notificationTokenList.add(notificationToken)
                notificationTokenDao.insert(notificationToken)
            }
        }
        return notificationTokenList
    }

    suspend fun upsert(
        userId: String,
        notificationToken: NotificationToken
    ): Resource {
        val isNotificationTokenExistingQuery = userCollectionRef
            .document(userId)
            .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
            .get()
            .await()

        if (isNotificationTokenExistingQuery.documents.isNotEmpty()) {
            for (document in isNotificationTokenExistingQuery.documents) {
                if (document["token"].toString() == notificationToken.token) {
                    return Resource.Error("Failed", false)
                }
            }
        } else {
            var isCreated = false
            userCollectionRef
                .document(userId)
                .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                .add(notificationToken)
                .addOnSuccessListener {
                    isCreated = true
                }.addOnFailureListener {
                }
            return Resource.Success("Success", isCreated)
        }
        return Resource.Error("Failed", false)
    }
}
