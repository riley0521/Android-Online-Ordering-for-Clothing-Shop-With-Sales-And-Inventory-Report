package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.*
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl
): ViewModel() {

    private val _cart = MutableLiveData<List<Cart>>()
    val cart: LiveData<List<Cart>> = _cart

    // TODO("I think I should not get the cart in startup of the application, get the cart instead in the cart fragment to reduce memory/network usage")
    fun getCart(userId: String) {
        _cart.value = cartRepository.getCartByUserId(userId).asLiveData(Dispatchers.IO, 10000).value
    }

    fun updateCartItemQty(userId: String, cartId: String, flag: String) = viewModelScope.launch {
        cartRepository.updateCartQuantity(userId, cartId, flag)
    }
}