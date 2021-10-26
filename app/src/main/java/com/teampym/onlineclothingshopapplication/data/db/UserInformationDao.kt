package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.UserInformation

@Dao
interface UserInformationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(userInformationDao: UserInformationDao)

    @Query("SELECT * FROM table_users")
    suspend fun getAll(): List<UserInformation>

    @Query("SELECT * FROM table_users WHERE userId = :userId")
    suspend fun getCurrentUser(userId: String): UserInformation

    @Update
    suspend fun update(userInformationDao: UserInformationDao)

    @Delete
    suspend fun delete(userInformation: UserInformation)

    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getCurrentUserWithDeliveryInfo(userId: String): List<UserWithDeliveryInfo>

    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getCurrentUserWithTokens(userId: String): List<UserWithTokens>

    @Transaction
    @Query("SELECT * FROM table_users WHERE userId = :userId")
    fun getCurrentUserWithCart(userId: String): List<UserWithCart>

}