package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Update
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken

@Dao
interface NotificationTokenDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(notificationToken: NotificationToken)

    @Update
    suspend fun update(notificationToken: NotificationToken)

    @Delete
    suspend fun delete(notificationToken: NotificationToken)

}