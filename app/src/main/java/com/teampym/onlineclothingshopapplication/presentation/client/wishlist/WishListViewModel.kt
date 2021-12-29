package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WishListViewModel @Inject constructor(
    private val wishItemDao: WishItemDao
) : ViewModel() {

    fun getAllWishList(userId: String) = wishItemDao.getAll(userId)

}