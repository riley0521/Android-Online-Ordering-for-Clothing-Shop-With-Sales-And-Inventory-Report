package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WishItemDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(wishItem: WishItem)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(wishList: List<WishItem>)

    @Query("SELECT * FROM table_wish_list WHERE userId = :userId")
    fun getAll(userId: String): Flow<List<WishItem>>

    @Query("DELETE FROM table_wish_list WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM table_wish_list WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
