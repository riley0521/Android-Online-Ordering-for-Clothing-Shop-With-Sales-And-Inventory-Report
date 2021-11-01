package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation

@Dao
interface DeliveryInformationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(deliveryInformation: DeliveryInformation)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(vararg deliveryInformation: DeliveryInformation)

    @Update
    suspend fun update(deliveryInformation: DeliveryInformation)

    @Query("DELETE FROM table_users WHERE userId = :userId")
    suspend fun delete(userId: String)
}