package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryInformationDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(deliveryInformation: DeliveryInformation)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(deliveryInformation: List<DeliveryInformation>)

    @Query("SELECT * FROM table_delivery_info WHERE userId = :userId")
    fun getAll(userId: String): Flow<List<DeliveryInformation>>

    @Update
    suspend fun update(deliveryInformation: DeliveryInformation)

    @Query("DELETE FROM table_users WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM table_delivery_info")
    suspend fun deleteAll()
}