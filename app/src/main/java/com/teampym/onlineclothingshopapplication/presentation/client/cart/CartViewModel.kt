package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.*
import com.teampym.onlineclothingshopapplication.USER_ID_KEY
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl,
    private val userInformationDao: UserInformationDao
): ViewModel() {

    private val _cart = MutableLiveData<List<Cart>>()
    val cart: LiveData<List<Cart>> = _cart

    val userInformation = userInformationDao.getCurrentUser(Utils.userId).asLiveData()

    // TODO("I think I should not get the cart in startup of the application, get the cart instead in the cart fragment to reduce memory/network usage")
    fun getCart(userId: String) {
        _cart.value = cartRepository.getAll(userId).asLiveData(Dispatchers.IO, 10000).value
    }

    fun updateCartItemQty(userId: String, cartId: String, flag: String) = viewModelScope.launch {
        cartRepository.updateQty(userId, cartId, flag)
    }
}