package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckOutSharedViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val cartDao: CartDao,
    private val orderRepository: OrderRepositoryImpl,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var _userWithDeliveryInfo = MutableLiveData<UserWithDeliveryInfo?>()
    private var _userWithNotificationsTokens = MutableLiveData<UserWithNotificationTokens?>()

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod>()

    private val _checkOutCartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        _selectedPaymentMethod.value = sessionPref.paymentMethod

        _userWithDeliveryInfo.value = userInformationDao.getUserWithDeliveryInfo()
            .firstOrNull { it.user.userId == sessionPref.userId }

        _userWithNotificationsTokens.value = userInformationDao.getUserWithNotificationTokens()
            .firstOrNull { it.user.userId == sessionPref.userId }

        cartDao.getAll(sessionPref.userId)
    }

    val selectedPaymentMethod: LiveData<PaymentMethod> get() = _selectedPaymentMethod

    val userWithDeliveryInfo: LiveData<UserWithDeliveryInfo?> get() = _userWithDeliveryInfo

    // I think I will remove this because what I need is the notification token of admins to notify them.
    val userWithNotificationTokens: LiveData<UserWithNotificationTokens?> get() = _userWithNotificationsTokens

    val finalCartList = _checkOutCartFlow.asLiveData()

    val order = MutableLiveData<Order?>()

    fun placeOrder(
        userInformation: UserInformation,
        cartList: List<Cart>,
        paymentMethod: String
    ) = viewModelScope.launch {
        order.value = orderRepository.create(
            userInformation.userId,
            cartList,
            userInformation.deliveryInformationList.first { it.isPrimary },
            paymentMethod
        )
    }

    //region For Select Payment Fragment
    fun onPaymentMethodSelected(paymentMethod: PaymentMethod) = viewModelScope.launch {
        preferencesManager.updatePaymentMethod(paymentMethod)
    }
    //endregion
}