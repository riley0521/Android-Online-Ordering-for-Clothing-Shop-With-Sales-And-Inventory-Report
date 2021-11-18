package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.flow.Flow

@Dao
interface RegionDao {

    @Query("SELECT * FROM table_regions")
    fun getAll(): Flow<List<Region>>

    @Transaction
    @Query("SELECT * FROM table_regions")
    fun getRegionsWithProvinces(): List<RegionWithProvinces>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(region: Region)

    @Update
    suspend fun update(region: Region)

    @Delete
    suspend fun delete(region: Region)
}
