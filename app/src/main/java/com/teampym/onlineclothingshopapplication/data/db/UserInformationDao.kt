package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import java.math.BigDecimal

@Dao
interface UserInformationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(userInformation: UserInformation)

    @Query("SELECT * FROM table_users")
    suspend fun getAll(): List<UserInformation>

    @Query("SELECT * FROM table_users WHERE userId = :userId")
    suspend fun getCurrentUser(userId: String): UserInformation

    @Query("UPDATE table_users SET totalOfCart = :totalOfCart WHERE userId = :userId")
    suspend fun updateTotalOfCart(userId: String, totalOfCart: Double)

    @Update
    suspend fun update(userInformation: UserInformation)

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