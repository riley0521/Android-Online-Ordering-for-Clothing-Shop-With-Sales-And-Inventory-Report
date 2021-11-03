package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.*
import com.teampym.onlineclothingshopapplication.USER_ID_KEY
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    @ApplicationScope val appScope: CoroutineScope
): ViewModel() {

    val userInformation = userInformationDao.getCurrentUser(Utils.userId).asLiveData()

    private val _cart = cartRepository.getAll(Utils.userId).asLiveData()
    val cart: MutableLiveData<MutableList<Cart>> = _cart as MutableLiveData<MutableList<Cart>>

    fun updateQty(cartId: String, flag: String) = viewModelScope.launch {
        cart.value?.first { it.id == cartId }?.let {
            when(flag) {
                CartFlag.ADDING.toString() -> {
                    it.quantity += 1
                    it.subTotal = it.calculatedTotalPrice.toDouble()
                }
                CartFlag.REMOVING.toString() -> {
                    it.quantity -= 1
                    it.subTotal = it.calculatedTotalPrice.toDouble()
                }
            }
        }
        cart.value = cart.value
    }

    fun updateCart(userId: String, cart: List<Cart>) = appScope.launch {
        cartRepository.update(userId, cart)
    }
}