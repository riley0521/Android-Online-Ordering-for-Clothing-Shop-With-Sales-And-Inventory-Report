package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Update
import com.teampym.onlineclothingshopapplication.data.models.Cart

@Dao
interface CartDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(cart: Cart)

    @Update
    suspend fun update(cart: Cart)

    @Delete
    suspend fun delete(cart: Cart)

}