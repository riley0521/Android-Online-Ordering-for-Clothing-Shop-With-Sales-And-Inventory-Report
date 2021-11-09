package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.db.CartDao
import com.teampym.onlineclothingshopapplication.data.db.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.db.UserWithDeliveryInfoAndTokens
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

    lateinit var user: LiveData<UserWithDeliveryInfoAndTokens>

    val selectedPaymentMethod = MutableLiveData("")

    private val cartFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        selectedPaymentMethod.value = sessionPref.paymentMethod.toString()

        user = userInformationDao.getUserWithDeliveryInfoAndTokens(sessionPref.userId).asLiveData()
        cartDao.getAll(sessionPref.userId)
    }

    val cart = cartFlow.asLiveData()

    val order = MutableLiveData<Order>()

    fun placeOrder(userInformation: UserInformation, paymentMethod: String) = viewModelScope.launch {
        order.value = orderRepository.create(userInformation, paymentMethod)
    }
}
