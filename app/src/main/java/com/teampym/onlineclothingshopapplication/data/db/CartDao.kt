package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.* // ktlint-disable no-wildcard-imports
import androidx.room.OnConflictStrategy.REPLACE
import com.teampym.onlineclothingshopapplication.data.models.Cart
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(cart: Cart)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(cart: List<Cart>)

    @Transaction
    @Query("SELECT * FROM table_cart WHERE userId = :userId")
    fun getAll(userId: String): Flow<List<Cart>>

    @Update
    suspend fun update(cart: Cart)

    @Query("DELETE FROM table_cart WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("DELETE FROM table_cart")
    suspend fun deleteAll()
}
