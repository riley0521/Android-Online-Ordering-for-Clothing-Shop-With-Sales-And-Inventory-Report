package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CartViewModel"

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val cartDao: CartDao,
    preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _cartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        cartRepository.getAll(sessionPref.userId)
    }

    val cart = _cartFlow.asLiveData() as MutableLiveData<MutableList<Cart>>

    fun onQuantityUpdated(cartId: String, flag: String) = viewModelScope.launch {
        cart.value?.forEach {
            if (it.id == cartId) {
                when (flag) {
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
        }
        cart.value = cart.value
    }

    fun onCartUpdated(userId: String) = appScope.launch {
        val res = cartRepository.update(userId, cart.value!!)
        if (res) {
            async {
                cartDao.insertAll(cart.value!!)
            }.await()
        }
    }

    fun onDeleteOutOfStockItems(userId: String, cartList: List<Cart>) = viewModelScope.launch {
        val res = cartRepository.deleteOutOfStockItems(userId, cartList)
        if (res) {
            async { cartDao.deleteAllOutOfStockItems(userId) }.await()
        }
    }

    fun onDeleteItemSelected(userId: String, cartId: String) = viewModelScope.launch {
        val res = cartRepository.delete(userId, cartId)
        if (res) {
            async { cartDao.delete(cartId) }.await()
        }
    }
}
