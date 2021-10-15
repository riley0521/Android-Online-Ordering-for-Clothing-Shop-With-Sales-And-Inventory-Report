package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Cart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(): ViewModel() {

    var cartFlow = emptyFlow<List<Cart>>()

    fun getCartByUserId(userId: String) {
//        cartFlow = if(userId.isNotEmpty())
//            cartDao.getAllCartItemsWithUserId(userId)
//        else
//            cartDao.getAllCartItemsWithoutUserId()
    }

    fun updateCartItemQty(cart: Cart, qty: Long) = viewModelScope.launch {
//        cartDao.update(cart.copy(quantity = qty))
    }
}