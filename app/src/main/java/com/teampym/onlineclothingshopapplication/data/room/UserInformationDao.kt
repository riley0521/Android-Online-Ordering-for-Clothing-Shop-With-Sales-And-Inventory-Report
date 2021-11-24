package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.* // ktlint-disable no-wildcard-imports
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInformationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(userInformation: UserInformation)

    @Query("SELECT * FROM table_users")
    fun getAll(): Flow<List<UserInformation>>

    @Query("SELECT * FROM table_users WHERE userId = :userId")
    suspend fun getCurrentUser(userId: String): UserInformation?

    @Query("UPDATE table_users SET firstName = :firstName, lastName = :lastName, birthDate = :birthDate WHERE userId = :userId")
    suspend fun updateBasicInfo(firstName: String, lastName: String, birthDate: String, userId: String)

    @Query("DELETE FROM table_users WHERE userId = :userId")
    suspend fun deleteAll(userId: String)

    @Transaction
    @Query("SELECT * FROM table_users")
    suspend fun getUserWithDeliveryInfo(): List<UserWithDeliveryInfo>

    @Transaction
    @Query("SELECT * FROM table_users")
    suspend fun getUserWithNotificationTokens(): List<UserWithNotificationTokens>

    @Transaction
    @Query("SELECT * FROM table_users")
    suspend fun getUserWithWishList(): List<UserWithWishList>
}
