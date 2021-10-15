package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*

@Dao
interface RegionDao {

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