package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.Product

@Dao
interface ProductDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(product: Product)
}
