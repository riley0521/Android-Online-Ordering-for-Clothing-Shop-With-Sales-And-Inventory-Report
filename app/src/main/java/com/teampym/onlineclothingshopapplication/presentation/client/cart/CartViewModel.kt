package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val cartDao: CartDao,
    preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private var _userId = MutableLiveData("")
    val userId: LiveData<String> get() = _userId

    private val _cartChannel = Channel<CartEvent>()
    val cartEvent = _cartChannel.receiveAsFlow()

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

    fun onCartUpdated(userId: String, cart: List<Cart>) = appScope.launch {
        val res = async { cartRepository.update(userId, cart) }.await()
        if (res) {
            async { cartDao.insertAll(cart) }.await()
            _cartChannel.send(CartEvent.NavigateToCheckOutFragment)
        }
    }

    fun onDeleteOutOfStockItems(cartList: List<Cart>) = appScope.launch {
        if (_userId.value != null) {
            val res = async {
                cartRepository.deleteOutOfStockItems(_userId.value!!, cartList)
            }.await()
            if (res) {
                cartDao.deleteAllOutOfStockItems(_userId.value!!)
            }
        }
    }

    fun onDeleteItemSelected(userId: String, cartId: String) = viewModelScope.launch {
        val res = async { cartRepository.delete(userId, cartId) }.await()
        if (res) {
            async { cartDao.delete(cartId) }.await()
            _cartChannel.send(CartEvent.ShowMessage("Item deleted successfully!"))
        }
    }

    sealed class CartEvent {
        data class ShowMessage(val msg: String) : CartEvent()
        object NavigateToCheckOutFragment : CartEvent()
    }
}
