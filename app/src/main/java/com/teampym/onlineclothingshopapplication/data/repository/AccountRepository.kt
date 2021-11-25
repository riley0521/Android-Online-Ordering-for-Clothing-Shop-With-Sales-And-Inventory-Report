package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun get(
        userId: String
    ): UserInformation? {
        return withContext(dispatcher) {
            var fetchedUser: UserInformation? = null
            val userQuery = userCollectionRef.document(userId)
                .get()
                .await()

            userQuery?.let { doc ->

                val userInfo =
                    doc.toObject(UserInformation::class.java)!!.copy(userId = doc.id)

                fetchedUser = userInfo.copy()
            }
            fetchedUser
        }
    }

    suspend fun create(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): UserInformation? {
        return withContext(dispatcher) {
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
                    }.addOnFailureListener {
                        newUser = null
                        return@addOnFailureListener
                    }
            }
            newUser
        }
    }

    suspend fun update(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            if (firstName.isNotEmpty() && birthDate.isNotEmpty()) {

                val date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthDate)
                val calendarDate = Calendar.getInstance()
                date?.let { calendarDate.time = it }

                // Check if the user is in the right age to use the application
                if (Calendar.getInstance().get(Calendar.YEAR)
                    .minus(calendarDate.get(Calendar.YEAR)) >= 12
                ) {
                    val userDocument = userCollectionRef
                        .document(userId)
                        .get()
                        .await()
                    val fetchedUser = userDocument
                        .toObject<UserInformation>()!!.copy(userId = userDocument.id)

                    fetchedUser.firstName = firstName
                    fetchedUser.lastName = lastName
                    fetchedUser.birthDate = birthDate
                    fetchedUser.dateModified = System.currentTimeMillis()

                    userCollectionRef
                        .document(userId)
                        .set(fetchedUser, SetOptions.merge())
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            isSuccessful = false
                            return@addOnFailureListener
                        }
                }
            }
            isSuccessful
        }
    }
}
