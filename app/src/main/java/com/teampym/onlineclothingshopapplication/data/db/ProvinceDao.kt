package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*

@Dao
interface ProvinceDao {

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