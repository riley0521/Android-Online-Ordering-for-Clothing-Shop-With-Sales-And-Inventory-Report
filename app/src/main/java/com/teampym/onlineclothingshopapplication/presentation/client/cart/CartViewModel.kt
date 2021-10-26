package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(): ViewModel() {

    var cartFlow = emptyFlow<List<Cart>>()

    // TODO("I think I should not get the cart in startup of the application, get the cart instead in the cart fragment to reduce memory/network usage")

    fun getCart(userId: String) {
        cartFlow = flowOf(Utils.currentUser!!.cart!!)
    }

    fun updateCartItemQty(cart: Cart, qty: Long) = viewModelScope.launch {
//        cartDao.update(cart.copy(quantity = qty))
    }
}