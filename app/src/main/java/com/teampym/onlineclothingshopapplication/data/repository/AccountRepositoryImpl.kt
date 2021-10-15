package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.auth.UserInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


class AccountRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    suspend fun createUser(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String,
        contactInformation: String
    ): UserInformation {
        val newUser = UserInformation(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            avatarUrl = avatarUrl,
            contactInformation = contactInformation,
            userType = "Client"
        )

        val result = db.collection("Users")
            .add(newUser)
            .await()

        return if(result != null) {
            newUser.copy(id = result.id)
        } else {
            UserInformation()
        }
    }

    suspend fun updateUser(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): UserInformation {

        val userToUpdate = db.collection("Users").document(userId)
        userToUpdate.get()
            .addOnSuccessListener {

            }

        return UserInformation()
    }
}