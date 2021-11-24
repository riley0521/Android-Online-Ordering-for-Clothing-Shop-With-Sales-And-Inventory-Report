package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "NTRepository"

class NotificationTokenRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    val notificationTokenDao: NotificationTokenDao,
    val preferencesManager: PreferencesManager
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): List<NotificationToken> {

        val notificationTokenListTemp = mutableListOf<NotificationToken>()

        val notificationTokenQuery = userCollectionRef
            .document(userId)
            .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
            .get()
            .await()

        if (notificationTokenQuery.documents.isNotEmpty()) {
            for (document in notificationTokenQuery.documents) {
                val notificationToken = document
                    .toObject<NotificationToken>()!!.copy(id = document.id)

                notificationTokenListTemp.add(notificationToken)
            }
            notificationTokenDao.insertAll(notificationTokenListTemp)
        }
        return notificationTokenListTemp
    }

    suspend fun insert(
        userId: String,
        userType: String,
        token: String
    ): NotificationToken? {
        var createdTokenTemp: NotificationToken? = NotificationToken(
            userId = userId,
            token = token,
            dateModified = System.currentTimeMillis(),
            userType = userType
        )

        val isNotificationTokenExistingQuery = userCollectionRef
            .document(userId)
            .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
            .whereEqualTo("token", token)
            .limit(1)
            .get()
            .await()

        if (isNotificationTokenExistingQuery.documents.isNotEmpty()) {
            if (isNotificationTokenExistingQuery.documents[0]["token"].toString() == token) {
                createdTokenTemp = null
            }
        } else {
            createdTokenTemp?.let { nf ->
                val result = userCollectionRef
                    .document(userId)
                    .collection(NOTIFICATION_TOKENS_SUB_COLLECTION)
                    .add(nf)
                    .await()
                if (result != null) {
                    createdTokenTemp?.id = result.id
                } else {
                    createdTokenTemp = null
                }
            }
        }
        return createdTokenTemp
    }
}
