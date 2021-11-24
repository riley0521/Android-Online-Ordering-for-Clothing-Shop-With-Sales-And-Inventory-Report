package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val wishItemRepository: WishItemRepositoryImpl,
    val userInformationDao: UserInformationDao
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun get(
        userId: String
    ): UserInformation? {
        val userQuery = userCollectionRef.document(userId)
            .get()
            .await()

        userQuery?.let { doc ->

            val userInfo =
                doc.toObject(UserInformation::class.java)!!.copy(userId = doc.id)

            val wishList = wishItemRepository.getAll(doc.id)

            val finalUser = userInfo.copy(
                wishList = wishList
            )
            userInformationDao.insert(finalUser)

            return finalUser
        }
        return null
    }

    suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): UserInformation? {
        val createdUser = withContext(Dispatchers.IO) {
            var newUser: UserInformation? = UserInformation(
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate,
                avatarUrl = avatarUrl,
                userId = userId
            )

            newUser?.let { user ->
                userCollectionRef
                    .document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        runBlocking {
                            userInformationDao.insert(user)
                        }
                    }.addOnFailureListener {
                        newUser = null
                        return@addOnFailureListener
                    }
            }
            newUser
        }
        return createdUser
    }

    suspend fun update(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true

            if (firstName.isNotEmpty() && birthDate.isNotEmpty()) {

                val date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthDate)
                val calendarDate = Calendar.getInstance()
                calendarDate.time = date!!

                // Check if the user is in the right age to use the application
                if (Calendar.getInstance().get(Calendar.YEAR)
                    .minus(calendarDate.get(Calendar.YEAR)) >= 12
                ) {
                    val userMapToUpdate = mapOf<String, Any>(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "birthDate" to birthDate
                    )

                    userCollectionRef
                        .document(userId)
                        .set(userMapToUpdate, SetOptions.merge())
                        .addOnSuccessListener {
                            runBlocking {
                                userInformationDao.updateBasicInfo(
                                    firstName,
                                    lastName,
                                    birthDate,
                                    userId
                                )
                            }
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }
}
