package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.* // ktlint-disable no-wildcard-imports
import androidx.room.OnConflictStrategy.REPLACE
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

    @Query("DELETE FROM table_delivery_info WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM table_delivery_info WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
