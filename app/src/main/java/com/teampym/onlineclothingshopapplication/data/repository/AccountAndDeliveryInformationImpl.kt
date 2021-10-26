package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class AccountAndDeliveryInformationImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val userCollectionRef = db.collection("Users")

    // db.collection("Users").document(<id here>).collection("deliveryInformation").document(<id here>).get().await()
    // db.collection("Users").document(<id here>).collection("notificationTokens").document(<id here>).get().await()
    // db.collection("Users").document(<id here>).collection("cart").document(<id here>).get().await()

    suspend fun getUser(
        userId:String
    ): Boolean {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        if(userQuery.exists()) {

            // get all delivery information
            val deliveryInformationQuery = userCollectionRef.document(userId).collection("deliveryInformation").get().await()
            val deliveryInformationList = mutableListOf<DeliveryInformation>()
            if(deliveryInformationQuery.documents.isNotEmpty()) {
               for(document in deliveryInformationQuery) {
                   deliveryInformationList.add(
                       DeliveryInformation(
                           id = document.id,
                           userId = userId,
                           contactNo = document["contactNo"].toString(),
                           region = document["region"].toString(),
                           province = document["province"].toString(),
                           city = document["city"].toString(),
                           postalCode = document["postalCode"].toString(),
                           streetNumber = document["streetNumber"].toString(),
                       )
                   )
               }
            }

            // get all notificationTokens
            val notificationTokensQuery = userCollectionRef.document(userId).collection("notificationTokens").get().await()
            val notificationTokenList = mutableListOf<NotificationToken>()
            if(notificationTokensQuery.documents.isNotEmpty()) {
                for(document in notificationTokensQuery) {
                    notificationTokenList.add(
                        NotificationToken(
                            id = document.id,
                            userId = userId,
                            token = document["token"].toString()
                        )
                    )
                }
            }

            Utils.currentUser = UserInformation(
                userId = userId,
                firstName = userQuery["firstName"].toString(),
                lastName = userQuery["lastName"].toString(),
                deliveryInformation = deliveryInformationList,
                birthDate = userQuery["birthDate"].toString(),
                avatarUrl = userQuery["avatarUrl"].toString(),
                userType = userQuery["userType"].toString(),
                notificationTokens = notificationTokenList,
                cart = null,
                totalOfCart = userQuery["totalOfCart"].toString().toBigDecimal()
            )
            return true
        }
        return false
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

        return if(result != null) {
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

        if(firstName.isNotEmpty() && birthDate.isNotEmpty()) {

            val date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthDate)
            val calendarDate = Calendar.getInstance()
            calendarDate.time = date!!

            // Check if the user is in the right age to use the application
            if(Calendar.getInstance().get(Calendar.YEAR).minus(calendarDate.get(Calendar.YEAR)) >= 12) {
                userMapToUpdate["firstName"] = firstName
                userMapToUpdate["lastName"] = lastName
                userMapToUpdate["birthDate"] = birthDate

                val userQuery = userCollectionRef.document(userId).get().await()
                if(userQuery.exists()) {
                    userCollectionRef.document(userId).set(userMapToUpdate, SetOptions.merge()).await()
                    Utils.currentUser = Utils.currentUser?.copy(firstName = firstName, lastName = lastName, birthDate = birthDate)
                }
            }
        }
    }
}