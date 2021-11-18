package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.flow.Flow

@Dao
interface ProvinceDao {

    @Query("SELECT * FROM table_provinces WHERE regionId = :regionId")
    fun getAll(regionId: Long): Flow<List<Province>>

    @Transaction
    @Query("SELECT * FROM table_provinces")
    fun getProvincesWithCities(): List<ProvinceWithCities>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(province: Province)

    @Update
    suspend fun update(province: Province)

    @Delete
    suspend fun delete(province: Province)
}
