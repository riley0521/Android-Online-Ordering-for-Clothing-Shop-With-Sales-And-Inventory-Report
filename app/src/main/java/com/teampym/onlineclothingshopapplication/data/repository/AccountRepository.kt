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
import java.lang.Exception
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
            val newUser = UserInformation(
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate,
                avatarUrl = avatarUrl,
                userId = userId
            )

            try {
                userCollectionRef
                    .document(userId)
                    .set(newUser)
                    .await()

                return@withContext newUser
            } catch (ex: Exception) {
                return@withContext null
            }
        }
    }

    // Account Repository
    // Update the user information
    suspend fun update(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        avatarUrl: String
    ): Boolean {
        return withContext(dispatcher) {
            val isValid = firstName.isNotEmpty() && birthDate.isNotEmpty()

            if (isValid) {
                // Parsing the date string "01/01/2000" to a date object
                val date = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(birthDate)

                // Creating an instance of calendar object
                val calendarDate = Calendar.getInstance()
                // Set the time with the date variable
                calendarDate.time = date!!

                // Check if the user is in the right age to use the application
                // User should be 12 years old and above to access this app
                if (Calendar.getInstance().get(Calendar.YEAR)
                    .minus(calendarDate.get(Calendar.YEAR)) >= 12
                ) {

                    // Create a map to update fields in the firebase
                    val updateUserMap = mapOf<String, Any>(
                        "avatarUrl" to avatarUrl,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "birthDate" to birthDate,
                        "dateModified" to Utils.getTimeInMillisUTC()
                    )

                    // Execute the query in fireStore
                    // I'm using ktx gradle library
                    // This line executes. It reflects the changes in the fireStore
                    userCollectionRef
                        .document(userId)
                        .set(updateUserMap, SetOptions.merge())
                        .await()

                    return@withContext true
                }
            }
            // Returns false if the above operation fails
            false
        }
    }
}
