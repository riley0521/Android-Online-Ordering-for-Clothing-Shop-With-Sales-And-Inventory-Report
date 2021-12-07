package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.* // ktlint-disable no-wildcard-imports
import androidx.room.OnConflictStrategy.REPLACE
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
    suspend fun deleteAll(userId: String)

    @Query("DELETE FROM table_cart WHERE userId = :userId AND stock == 0")
    suspend fun deleteAllOutOfStockItems(userId: String)

    @Query("DELETE FROM table_cart WHERE id = :id")
    suspend fun delete(id: String)
}
