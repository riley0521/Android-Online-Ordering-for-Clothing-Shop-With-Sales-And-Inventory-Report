package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishListViewModel @Inject constructor(
    private val wishItemDao: WishItemDao
) : ViewModel() {

    private val _wishList = MutableLiveData<List<WishItem>>()
    val wishList: LiveData<List<WishItem>> get() = _wishList

    fun getAllWishList(userId: String) = viewModelScope.launch {
        _wishList.value = wishItemDao.getAll(userId)
    }
}
