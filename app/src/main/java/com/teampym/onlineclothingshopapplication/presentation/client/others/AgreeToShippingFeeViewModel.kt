package com.teampym.onlineclothingshopapplication.presentation.client.others

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgreeToShippingFeeViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private var userId = ""

    private var _hasAgreed = MutableLiveData(false)
    val hasAgreed: LiveData<Boolean> get() = _hasAgreed

    private var _isCanceled = MutableLiveData(false)
    val isCanceled: LiveData<Boolean> get() = _isCanceled

    fun agreeToSf(order: Order) = viewModelScope.launch {
        _hasAgreed.value = orderRepository.agreeToShippingFee(
            userId,
            order.id
        )
    }

    fun cancelOrder(order: Order) = viewModelScope.launch {
        _isCanceled.value = orderRepository.cancelOrder(
            order.deliveryInformation.name,
            order.id
        )
    }
}
