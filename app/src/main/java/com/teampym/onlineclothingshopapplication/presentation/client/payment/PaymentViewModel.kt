package com.teampym.onlineclothingshopapplication.presentation.client.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _paymentChannel = Channel<PaymentEvent>()
    val paymentEvent = _paymentChannel.receiveAsFlow()

    fun updateOrder(orderId: String) = viewModelScope.launch {
        val res = orderRepository.updateOrderToPaid(orderId)
        if (res) {
            _paymentChannel.send(PaymentEvent.ShowMessage("Payment successful!"))
        } else {
            _paymentChannel.send(PaymentEvent.ShowMessage("Payment not approved."))
        }
    }

    sealed class PaymentEvent {
        data class ShowMessage(val msg: String) : PaymentEvent()
    }
}
