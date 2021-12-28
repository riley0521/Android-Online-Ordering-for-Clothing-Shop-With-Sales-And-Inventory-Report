package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.Cart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckOutSharedViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val cartDao: CartDao,
    private val orderRepository: OrderRepository,
    private val cartRepository: CartRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod>()

    private val _checkOutChannel = Channel<CheckOutEvent>()
    val checkOutEvent = _checkOutChannel.receiveAsFlow()

    val userWithDeliveryInfo =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            _selectedPaymentMethod.value = sessionPref.paymentMethod

            flowOf(
                userInformationDao.getUserWithDeliveryInfo()
                    .firstOrNull { it.user.userId == sessionPref.userId }
            )
        }

    val selectedPaymentMethod: LiveData<PaymentMethod> get() = _selectedPaymentMethod

    fun placeOrder(
        userInformation: UserInformation,
        cartList: List<Cart>,
        paymentMethod: String,
        additionalNote: String,
    ) = appScope.launch {
        val orderResult = async {
            orderRepository.create(
                userInformation.userId,
                cartList,
                userInformation.deliveryInformationList.first { it.isPrimary },
                paymentMethod,
                additionalNote
            )
        }.await()
        if (orderResult != null) {

            // Delete all items from cart in remote and local db
            async { cartRepository.deleteAll(userInformation.userId) }.await()
            async { cartDao.deleteAll(userInformation.userId) }.await()

            _checkOutChannel.send(CheckOutEvent.ShowSuccessfulMessage("Thank you for placing your order!"))
        } else {
            _checkOutChannel.send(CheckOutEvent.ShowFailedMessage("Failed to place order. Please try again later."))
        }
    }

    sealed class CheckOutEvent {
        data class ShowSuccessfulMessage(val msg: String) : CheckOutEvent()
        data class ShowFailedMessage(val msg: String) : CheckOutEvent()
    }

    //region For Select Payment Fragment
    fun onPaymentMethodSelected(paymentMethod: PaymentMethod) = viewModelScope.launch {
        preferencesManager.updatePaymentMethod(paymentMethod)
    }
    //endregion
}
