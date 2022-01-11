package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val cartDao: CartDao,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val _cartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        cartRepository.getAll(sessionPref.userId)
    }

    val cart = _cartFlow.asLiveData()

    private val _cartChannel = Channel<CartEvent>()
    val cartEvent = _cartChannel.receiveAsFlow()

    fun onQuantityUpdated(cartId: String, flag: String, position: Int) = viewModelScope.launch {
        cart.value?.map {
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
        _cartChannel.send(
            CartEvent.ItemModifiedOrRemoved(
                position,
                false,
                cart.value?.sumOf { it.calculatedTotalPrice.toDouble() } ?: 0.0
            )
        )
    }

    fun onDeleteOutOfStockItems(userId: String, cartList: List<Cart>) = viewModelScope.launch {
        val res = cartRepository.deleteOutOfStockItems(userId, cartList)
        if (res) {
            cartDao.deleteAllOutOfStockItems(userId)
        }
    }

    fun onDeleteItemSelected(userId: String, cartId: String, position: Int) = viewModelScope.launch {
        val res = cartRepository.delete(userId, cartId)
        if (res) {
            cartDao.delete(cartId)

            _cartChannel.send(
                CartEvent.ItemModifiedOrRemoved(
                    position,
                    true,
                    cart.value?.sumOf { it.calculatedTotalPrice.toDouble() } ?: 0.0
                )
            )
        }
    }

    sealed class CartEvent {
        data class ItemModifiedOrRemoved(
            val position: Int,
            val isRemoved: Boolean,
            val currentTotalPrice: Double
        ) : CartEvent()
    }
}
