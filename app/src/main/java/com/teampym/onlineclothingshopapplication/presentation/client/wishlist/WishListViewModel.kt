package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.repository.WishItemRepository
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishListViewModel @Inject constructor(
    private val wishItemRepository: WishItemRepository,
    private val wishItemDao: WishItemDao
) : ViewModel() {

    private val _wishList = MutableLiveData<List<WishItem>>()
    val wishList: LiveData<List<WishItem>> get() = _wishList

    private val _wishListChannel = Channel<WishListEvent>()
    val wishListEvent = _wishListChannel.receiveAsFlow()

    fun getAllWishList(userId: String) = viewModelScope.launch {
        _wishList.value = wishItemDao.getAll(userId)
    }

    fun removeFromWishList(item: WishItem, position: Int) = viewModelScope.launch {
        val res = wishItemRepository.remove(item.userId, item.productId)
        if (res) {
            wishItemDao.delete(item.productId)
            _wishListChannel.send(
                WishListEvent.ShowMessageAndNotifyAdapter(
                    "Product removed from wish list successfully!",
                    position
                )
            )
        } else {
            _wishListChannel.send(WishListEvent.ShowErrorMessage("Removing product from wish list failed. Please try again."))
        }
    }

    sealed class WishListEvent {
        data class ShowMessageAndNotifyAdapter(val msg: String, val position: Int) : WishListEvent()
        data class ShowErrorMessage(val msg: String) : WishListEvent()
    }
}
