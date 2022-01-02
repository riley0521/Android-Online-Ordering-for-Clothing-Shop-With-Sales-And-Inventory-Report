package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.teampym.onlineclothingshopapplication.data.models.WishItem

@Dao
interface WishItemDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(wishItem: WishItem)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(wishList: List<WishItem>)

    @Query("SELECT * FROM table_wish_list WHERE userId = :userId")
    suspend fun getAll(userId: String): List<WishItem>

    @Query("SELECT * FROM table_wish_list WHERE productId = :productId")
    suspend fun checkIfExisting(productId: String): WishItem?

    @Query("DELETE FROM table_wish_list WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM table_wish_list WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
