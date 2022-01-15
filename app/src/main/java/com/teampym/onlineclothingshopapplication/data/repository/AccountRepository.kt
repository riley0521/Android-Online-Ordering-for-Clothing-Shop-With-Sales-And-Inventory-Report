package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserStatus
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.presentation.admin.accounts.AccountPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    db: FirebaseFirestore,
    private val auditTrailRepository: AuditTrailRepository,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun get(
        userId: String
    ): UserInformation? {
        return withContext(dispatcher) {
            val userQuery = userCollectionRef.document(userId)
                .get()
                .await()

            if (userQuery.data != null) {
                userQuery.toObject<UserInformation>()!!.copy(userId = userQuery.id)
            } else {
                null
            }
        }
    }

    suspend fun checkIfUserIsBanned(userId: String): Boolean {
        return withContext(dispatcher) {
            val userInfo = get(userId)

            userInfo?.let {
                return@withContext userInfo.userStatus == UserStatus.BANNED.name
            }
            false
        }
    }

    fun getAll(queryAccounts: Query) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            AccountPagingSource(queryAccounts)
        }

    suspend fun banUser(username: String, user: UserInformation): Boolean {
        return withContext(dispatcher) {
            try {
                userCollectionRef
                    .document(user.userId)
                    .set(
                        mapOf(
                            "userStatus" to UserStatus.BANNED.name
                        ),
                        SetOptions.merge()
                    )
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username BANNED ${user.firstName} ${user.lastName} (${user.userId})",
                        type = AuditType.ACCOUNT.name
                    )
                )

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun unBanUser(username: String, user: UserInformation): Boolean {
        return withContext(dispatcher) {
            try {
                userCollectionRef
                    .document(user.userId)
                    .set(
                        mapOf(
                            "userStatus" to UserStatus.ACTIVE.name
                        ),
                        SetOptions.merge()
                    )
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UNBANNED ${user.firstName} ${user.lastName} (${user.userId})",
                        type = AuditType.ACCOUNT.name
                    )
                )

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
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
                    .set(newUser, SetOptions.merge())
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
