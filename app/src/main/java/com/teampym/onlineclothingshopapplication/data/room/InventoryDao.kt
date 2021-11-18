package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE

@Dao
interface InventoryDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(inventory: Inventory)
}
