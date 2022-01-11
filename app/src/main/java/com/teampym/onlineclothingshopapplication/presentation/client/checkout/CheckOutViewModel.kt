package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
import com.teampym.onlineclothingshopapplication.data.repository.NotificationTokenRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderDetailRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val cartDao: CartDao,
    private val orderRepository: OrderRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val cartRepository: CartRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _checkOutChannel = Channel<CheckOutEvent>()
    val checkOutEvent = _checkOutChannel.receiveAsFlow()

    val userWithDeliveryInfo =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            flowOf(
                userInformationDao.getUserWithDeliveryInfo()
                    .firstOrNull { it.user.userId == sessionPref.userId }
            )
        }

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod>()
    val selectedPaymentMethod: LiveData<PaymentMethod> get() = _selectedPaymentMethod

    fun fetchSelectPaymentMethod() = viewModelScope.launch {
        _selectedPaymentMethod.postValue(preferencesManager.preferencesFlow.first().paymentMethod)
    }

    fun placeOrder(
        userInformation: UserInformation,
        cartList: List<Cart>,
        additionalNote: String,
        paymentMethod: PaymentMethod
    ) = viewModelScope.launch {
        val createdOrder = orderRepository.create(
            userInformation.userId,
            cartList,
            userInformation.defaultDeliveryAddress,
            additionalNote,
            paymentMethod
        )
        if (createdOrder != null && cartList.isNotEmpty()) {

            orderDetailRepository.insertAll(
                createdOrder.id,
                userInformation.userId,
                cartList
            )

            // Delete all items from cart in remote and local db
            cartRepository.deleteAll(userInformation.userId)
            cartDao.deleteAll(userInformation.userId)

            val isNotified = notificationTokenRepository.notifyAllAdmins(
                createdOrder,
                "New Order (${createdOrder.id})",
                "You have a new order!"
            )

            if (isNotified) {
                _checkOutChannel.send(
                    CheckOutEvent.ShowSuccessfulMessage(
                        "Thank you for placing your order!\n" +
                            "Admins are notified."
                    )
                )
            } else {
                _checkOutChannel.send(CheckOutEvent.ShowSuccessfulMessage("Thank you for placing your order!"))
            }
        } else {
            _checkOutChannel.send(CheckOutEvent.ShowFailedMessage("Failed to place order. Please try again later."))
        }
    }

    sealed class CheckOutEvent {
        data class ShowSuccessfulMessage(val msg: String) : CheckOutEvent()
        data class ShowFailedMessage(val msg: String) : CheckOutEvent()
    }
}
