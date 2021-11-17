package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.util.Resource
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports
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
    ): UserInformation? {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        if (userQuery.data != null) {

            val userInfo = userQuery.toObject(UserInformation::class.java)!!.copy(userId = userId)

            val deliveryInformationList = deliveryInformationRepository.getAll(userQuery.id)

            val notificationTokenList = notificationTokenRepository.getAll(userQuery.id)

            val finalUser = userInfo.copy(
                deliveryInformationList = deliveryInformationList,
                notificationTokenList = notificationTokenList
            )
            userInformationDao.insert(finalUser)

            return finalUser
        }
        return null
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
            Resource.Error("Failed", UserInformation())
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
                    var isUpdated = false
                    userCollectionRef
                        .document(userId)
                        .set(userMapToUpdate, SetOptions.merge())
                        .addOnSuccessListener {
                            isUpdated = true
                            CoroutineScope(Dispatchers.IO).launch {
                                userInformationDao.updateBasicInfo(
                                    firstName,
                                    lastName,
                                    birthDate,
                                    userId
                                )
                            }
                        }.addOnFailureListener {
                        }

                    return Resource.Success("Success", isUpdated)
                }
            }
        }
        return Resource.Error("Failed", false)
    }
}
