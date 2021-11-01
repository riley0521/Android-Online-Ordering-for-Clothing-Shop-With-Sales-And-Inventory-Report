package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.*
import com.teampym.onlineclothingshopapplication.data.util.Resource
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class AccountRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val deliveryInformationRepository: DeliveryInformationRepositoryImpl,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    private val cartRepository: CartRepositoryImpl,
    private val userInformationDao: UserInformationDao
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun get(
        userId: String
    ): Resource {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        if (userQuery != null) {

            val userInfo = userQuery.toObject(UserInformation::class.java)
            if (userInfo != null) {
                val deliveryInformationList: List<DeliveryInformation> = when(val res = deliveryInformationRepository.getAll(userQuery.id)) {
                    is Resource.Error -> emptyList()
                    is Resource.Success -> res.res as List<DeliveryInformation>
                }

                val notificationTokenList: List<NotificationToken> = when(val res = notificationTokenRepository.getAll(userQuery.id)) {
                    is Resource.Error -> emptyList()
                    is Resource.Success -> res.res as List<NotificationToken>
                }

                val copy = userInfo.copy(userId = userQuery.id, deliveryInformationList = deliveryInformationList, notificationTokenList = notificationTokenList)

                userInformationDao.insert(copy)

                return Resource.Success("Success", copy)
            }
        }
        return Resource.Error("Failed", null)
    }

    // TODO("Should get firebase token as well while we're at it.")
    suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): Resource {
        val newUser = UserInformation(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            totalOfCart = 0.0,
            userId = userId
        )

        val result = userCollectionRef
            .document(userId)
            .set(newUser)
            .await()

        return if (result != null) {
            userInformationDao.insert(newUser)
            Resource.Success("Success", newUser)
        } else {
            Resource.Error("Failed", null)
        }
    }

    suspend fun update(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ): Resource {

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
                    return Resource.Success("Success", UserInformation())
                }
            }
        }
        return Resource.Error("Failed", null)
    }
}