package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.db.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val cartDao: CartDao,
    private val orderRepository: OrderRepositoryImpl,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var _userWithDeliveryInfo = MutableLiveData<UserWithDeliveryInfo>()
    private var _userWithNotificationsTokens = MutableLiveData<UserWithNotificationTokens>()

    private val _selectedPaymentMethod = MutableLiveData("")

    private val _cartList = MutableLiveData<List<CartWithProductAndInventory>>()

    private val _cartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        _selectedPaymentMethod.value = sessionPref.paymentMethod.toString()

        _userWithDeliveryInfo = userInformationDao.getUserWithDeliveryInfo(sessionPref.userId)
            .asLiveData() as MutableLiveData<UserWithDeliveryInfo>

        _userWithNotificationsTokens =
            userInformationDao.getUserWithNotificationTokens(sessionPref.userId)
            .asLiveData() as MutableLiveData<UserWithNotificationTokens>

        cartDao.getAll(sessionPref.userId)
    }

    val selectedPaymentMethod: LiveData<String> get() = _selectedPaymentMethod

    val userWithDeliveryInfo: LiveData<UserWithDeliveryInfo> get() = _userWithDeliveryInfo
    val userWithNotificationTokens: LiveData<UserWithNotificationTokens> get() = _userWithNotificationsTokens

    val cartList = _cartFlow.asLiveData()

    val order = MutableLiveData<Order>()

    fun placeOrder(
        userInformation: UserInformation,
        cartList: List<Cart>,
        paymentMethod: String
    ) = viewModelScope.launch {
        order.value = orderRepository.create(
            userInformation.userId,
            cartList,
            userInformation.deliveryInformationList.first { it.default },
            paymentMethod
        )
    }
}
