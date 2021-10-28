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

    // db.collection("Users").document(<id here>).collection("deliveryInformation").document(<id here>).get().await()
    // db.collection("Users").document(<id here>).collection("notificationTokens").document(<id here>).get().await()
    // db.collection("Users").document(<id here>).collection("cart").document(<id here>).get().await()

    suspend fun getUser(
        userId: String
    ): UserInformation? {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        if (userQuery.exists()) {

            val obj = userQuery.toObject(UserInformation::class.java)

            obj?.let { userInfo ->

                userInformationDao.insert(userInfo)
                userInfo.deliveryInformation!!.forEach {
                    deliveryInformationDao.insert(it)
                }
                userInfo.notificationTokens!!.forEach {
                    notificationTokenDao.insert(it)
                }

                return userInfo
            }
        }
        return null
    }

    suspend fun createUser(
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): UserInformation? {
        val newUser = UserInformation(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            avatarUrl = avatarUrl
        )

        // You can use this as well
//        val result = userCollectionRef
//            .document(UUID.randomUUID().toString())
//            .set(newUser)
//            .await()

        val result = userCollectionRef
            .add(newUser)
            .await()

        return if (result != null) {
            newUser.copy(userId = result.id)
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
                    Utils.currentUser = Utils.currentUser?.copy(
                        firstName = firstName,
                        lastName = lastName,
                        birthDate = birthDate
                    )
                }
            }
        }
    }
}