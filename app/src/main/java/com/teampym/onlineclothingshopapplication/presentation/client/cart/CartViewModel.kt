package com.teampym.onlineclothingshopapplication.presentation.client.cart

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val cartDao: CartDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _userInformation = MutableLiveData<UserInformation?>()

    private val _cartChannel = Channel<CartEvent>()
    val cartEvent = _cartChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    private val cartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        _userInformation.value = userInformationDao.getCurrentUser(sessionPref.userId)
        cartRepository.getAll(if (sessionPref.userId.isNotBlank()) sessionPref.userId else null)
    }

    val userInformation: LiveData<UserInformation?> get() = _userInformation

    @ExperimentalCoroutinesApi
    val cart = cartFlow.asLiveData() as MutableLiveData<MutableList<Cart>>

    @ExperimentalCoroutinesApi
    fun onQuantityUpdated(cartId: String, flag: String) = viewModelScope.launch {
        cart.value?.first { it.id == cartId }?.let {
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
        cart.value = cart.value
    }

    fun onCartUpdated(userId: String, cart: List<Cart>) = appScope.launch {
        if (cartRepository.update(userId, cart)) cartDao.insertAll(cart)
    }

    fun onDeleteOutOfStockItems(cartList: List<Cart>) = appScope.launch {
        userInformation.value?.userId?.let {
            if (cartRepository.deleteOutOfStockItems(it, cartList)) {
                cartDao.deleteAll(it)
            }
        }
    }

    fun onDeleteItemSelected(userId: String, cartId: String) = viewModelScope.launch {
        if (cartRepository.delete(userId, cartId)) {
            cartDao.delete(cartId)
            _cartChannel.send(CartEvent.ShowMessage("Item deleted successfully!"))
        }
    }

    sealed class CartEvent {
        data class ShowMessage(val msg: String) : CartEvent()
    }
}
