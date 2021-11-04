package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckOutViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val orderRepository: OrderRepositoryImpl
): ViewModel() {

    val user = userInformationDao.getUserWithDeliveryInfoAndTokens(Utils.userId).asLiveData()

    val order = MutableLiveData<Order>()

    fun placeOrder(userInformation: UserInformation, paymentMethod: String) = viewModelScope.launch {
        order.value = orderRepository.create(userInformation, paymentMethod)
    }
}