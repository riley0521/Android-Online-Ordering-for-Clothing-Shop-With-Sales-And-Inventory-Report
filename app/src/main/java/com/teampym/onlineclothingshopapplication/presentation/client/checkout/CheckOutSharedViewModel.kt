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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
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

    private var _userWithDeliveryInfo = MutableLiveData<UserWithDeliveryInfo?>()

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod>()

    private val _checkOutChannel = Channel<CheckOutEvent>()
    val checkOutEvent = _checkOutChannel.receiveAsFlow()

    private val _checkOutCartFlow =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            _selectedPaymentMethod.value = sessionPref.paymentMethod

            _userWithDeliveryInfo.value = userInformationDao.getUserWithDeliveryInfo()
                .firstOrNull { it.user.userId == sessionPref.userId }

            cartDao.getAll(sessionPref.userId)
        }

    val selectedPaymentMethod: LiveData<PaymentMethod> get() = _selectedPaymentMethod

    val userWithDeliveryInfo: LiveData<UserWithDeliveryInfo?> get() = _userWithDeliveryInfo

    val finalCartList = _checkOutCartFlow.asLiveData()

    suspend fun placeOrder(
        userInformation: UserInformation,
        cartList: List<Cart>,
        paymentMethod: String,
        additionalNote: String,
    ) = appScope.launch {
        val orderResult = orderRepository.create(
            userInformation.userId,
            cartList,
            userInformation.deliveryInformationList.first { it.isPrimary },
            paymentMethod,
            additionalNote
        )
        if (orderResult != null) {
            _checkOutChannel.send(CheckOutEvent.ShowSuccessfulMessage("Thank you for placing your order!"))

            // Delete all items from cart in remote and local db
            cartRepository.deleteAll(userInformation.userId)
            cartDao.deleteAll(userInformation.userId)
        } else {
            _checkOutChannel.send(CheckOutEvent.ShowFailedMessage("Failed to place order. Please try again later."))
        }
    }.join()

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
