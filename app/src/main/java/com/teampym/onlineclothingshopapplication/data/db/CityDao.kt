package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {

    @Query("SELECT * FROM table_cities WHERE provinceId = :provinceId")
    fun getAll(provinceId: Long): Flow<List<City>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: City)

    @Update
    suspend fun update(city: City)

    @Delete
    suspend fun delete(city: City)
}
