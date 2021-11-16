package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*
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

    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getUserFlow(userId: String): Flow<UserInformation?>

    @Query("UPDATE table_users SET firstName = :firstName, lastName = :lastName, birthDate = :birthDate WHERE userId = :userId")
    suspend fun updateBasicInfo(firstName: String, lastName: String, birthDate: String, userId: String)

    @Update
    suspend fun update(userInformation: UserInformation)

    @Query("DELETE FROM table_users WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM table_users")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getUserWithDeliveryInfo(userId: String): Flow<UserWithDeliveryInfo>

    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getUserWithNotificationTokens(userId: String): Flow<UserWithNotificationTokens>

    // I don't know if this necessary in the future. but I will leave it here for now.
    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getCurrentUserWithCart(userId: String): Flow<UserWithCart>
}
