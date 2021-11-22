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

    @Query("DELETE FROM table_wish_list WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
}
