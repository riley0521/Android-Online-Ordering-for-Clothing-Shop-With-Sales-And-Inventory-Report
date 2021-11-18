package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken

@Dao
interface NotificationTokenDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(notificationToken: NotificationToken)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(notificationTokens: List<NotificationToken>)

    @Update
    suspend fun update(notificationToken: NotificationToken)

    @Query("DELETE FROM table_tokens WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM table_tokens WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}