package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.*

@Dao
interface CityDao {

    @Query("SELECT * FROM table_cities")
    fun getAll(): List<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: City)

    @Update
    suspend fun update(city: City)

    @Delete
    suspend fun delete(city: City)

}