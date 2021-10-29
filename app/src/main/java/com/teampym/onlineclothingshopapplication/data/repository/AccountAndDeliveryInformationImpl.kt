package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class AccountAndDeliveryInformationImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val userInformationDao: UserInformationDao,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val notificationTokenDao: NotificationTokenDao
) {

    private val userCollectionRef = db.collection("Users")

    suspend fun getUser(
        userId: String
    ): UserInformation? {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        if (userQuery != null) {

            val userInfo = userQuery.toObject(UserInformation::class.java)
            if (userInfo != null) {
                val deliveryInformationQuery =
                    userCollectionRef.document(userQuery.id).collection("deliveryInformation").get()
                        .await()
                val deliveryInformationList = mutableListOf<DeliveryInformation>()
                deliveryInformationQuery?.let { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val deliveryInfo = document.toObject(DeliveryInformation::class.java)!!
                            .copy(id = document.id)
                        deliveryInformationList.add(deliveryInfo)
                        deliveryInformationDao.insert(deliveryInfo)
                    }
                }

                val notificationTokenQuery =
                    userCollectionRef.document(userQuery.id).collection("notificationTokens").get()
                        .await()
                val notificationTokenList = mutableListOf<NotificationToken>()
                notificationTokenQuery?.let { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val notificationToken =
                            document.toObject(NotificationToken::class.java)!!
                                .copy(id = document.id)
                        notificationTokenList.add(notificationToken)
                        notificationTokenDao.insert(notificationToken)
                    }
                }

                userInformationDao.insert(
                    userInfo.copy(
                        userId = userQuery.id
                    )
                )

                return userInfo.copy(userId = userQuery.id, deliveryInformationList = deliveryInformationList, notificationTokenList = notificationTokenList)
            }
        }
        return null
    }

    // getNotificationToken based on user id to notify them.
    suspend fun getNotificationTokensOfUser(userId: String): List<NotificationToken> {
        val notificationTokenQuery =
            userCollectionRef.document(userId).collection("notificationTokens").get().await()
        val notificationTokenList = mutableListOf<NotificationToken>()
        notificationTokenQuery?.let { querySnapshot ->
            for (document in querySnapshot.documents) {
                val notificationToken =
                    document.toObject(NotificationToken::class.java)!!.copy(id = document.id)
                notificationTokenList.add(notificationToken)
                notificationTokenDao.insert(notificationToken)
            }
        }
        return notificationTokenList
    }

    suspend fun upsertNotificationToken(
        userId: String,
        notificationToken: NotificationToken
    ): NotificationToken? {
        val isNotificationTokenExistingQuery =
            userCollectionRef.document(userId).collection("notificationTokens").get().await()
        if (isNotificationTokenExistingQuery != null) {
            for (document in isNotificationTokenExistingQuery.documents) {
                if (document["token"].toString() == notificationToken.token)
                    return document.toObject(NotificationToken::class.java)!!
                        .copy(id = document.id, token = "Existing")
            }
        } else {
            val result = userCollectionRef.document(userId).collection("notificationTokens")
                .add(notificationToken).await()
            if (result != null)
                return notificationToken.copy(id = result.id)
        }
        return null
    }

    suspend fun getDeliveryInformation(userId: String): List<DeliveryInformation>? {
        val deliveryInformationQuery =
            userCollectionRef.document(userId).collection("deliveryInformation").get().await()
        val deliveryInformationList = mutableListOf<DeliveryInformation>()
        deliveryInformationQuery?.let { querySnapshot ->
            for (document in querySnapshot.documents) {
                val deliveryInformation =
                    document.toObject(DeliveryInformation::class.java)!!.copy(id = document.id)
                deliveryInformationList.add(deliveryInformation)
                deliveryInformationDao.insert(deliveryInformation)
            }
            return deliveryInformationList
        }
        return null
    }

    suspend fun upsertDeliveryInformation(
        userId: String,
        deliveryInformation: DeliveryInformation
    ): DeliveryInformation? {
        val isDeliveryInformationExistingQuery = userCollectionRef.document(userId).collection("deliveryInformation").get().await()
        if(isDeliveryInformationExistingQuery != null) {
            for(document in isDeliveryInformationExistingQuery.documents) {
                val deliveryInfo = document.toObject(DeliveryInformation::class.java)!!.copy(id = document.id)
                if(deliveryInformation.contactNo == deliveryInfo.contactNo &&
                        deliveryInformation.region == deliveryInfo.region &&
                        deliveryInformation.streetNumber == deliveryInfo.streetNumber) {
                    return deliveryInfo.copy(userId = "Existing")
                }
            }
        } else {
            val result = userCollectionRef.document(userId).collection("deliveryInformation").add(deliveryInformation).await()
            if(result != null)
                return deliveryInformation.copy(id = result.id)
        }
        return null
    }

    // TODO("Should get firebase token as well while we're at it.")
    suspend fun createUser(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): UserInformation? {
        val newUser = UserInformation(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            userType = UserType.CUSTOMER.toString(),
            totalOfCart = 0.0,
            deliveryInformation = "",
            notificationTokens = "",
            cart = "",
            deliveryInformationList = emptyList(),
            notificationTokenList = emptyList(),
            cartList = emptyList()
        )

        val result = userCollectionRef
            .document(userId)
            .set(newUser)
            .await()

        return if (result != null) {
            userInformationDao.insert(newUser)
            newUser
        } else {
            null
        }
    }

    // TODO("updateUserAvatar() is for extra features soon.")
    // TODO("Create different methods per variable of user like updateUserBasicInformation, updateCart, updateUserAddress, updateUserPassword (if applicable)")
    suspend fun updateUserBasicInformation(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ) {

        val userMapToUpdate = mutableMapOf<String, Any>()

        if (firstName.isNotEmpty() && birthDate.isNotEmpty()) {

            val date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthDate)
            val calendarDate = Calendar.getInstance()
            calendarDate.time = date!!

            // Check if the user is in the right age to use the application
            if (Calendar.getInstance().get(Calendar.YEAR)
                    .minus(calendarDate.get(Calendar.YEAR)) >= 12
            ) {
                userMapToUpdate["firstName"] = firstName
                userMapToUpdate["lastName"] = lastName
                userMapToUpdate["birthDate"] = birthDate

                val userQuery = userCollectionRef.document(userId).get().await()
                if (userQuery.exists()) {
                    userCollectionRef.document(userId).set(userMapToUpdate, SetOptions.merge())
                        .await()
                    userInformationDao.updateBasicInfo(firstName, lastName, birthDate, userId)
                }
            }
        }
    }
}