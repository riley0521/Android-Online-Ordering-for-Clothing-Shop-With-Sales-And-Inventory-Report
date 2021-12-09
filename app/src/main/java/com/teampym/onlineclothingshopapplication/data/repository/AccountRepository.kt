package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
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

            if (userQuery.data != null) {
                val userInfo = userQuery
                    .toObject(UserInformation::class.java)!!.copy(userId = userQuery.id)

                fetchedUser = userInfo
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
                val res = userCollectionRef
                    .document(userId)
                    .set(user)
                    .await()
                if (res == null) {
                    newUser = null
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
                if (date != null) {
                    calendarDate.time = date

                    // Check if the user is in the right age to use the application
                    if (Calendar.getInstance().get(Calendar.YEAR)
                        .minus(calendarDate.get(Calendar.YEAR)) >= 12
                    ) {

                        val updateUserMap = mapOf<String, Any>(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "birthDate" to birthDate,
                            "dateModified" to Utils.getTimeInMillisUTC()
                        )

                        val result = userCollectionRef
                            .document(userId)
                            .set(updateUserMap, SetOptions.merge())
                            .await()
                        isSuccessful = result != null
                    }
                }
            }
            isSuccessful
        }
    }
}
