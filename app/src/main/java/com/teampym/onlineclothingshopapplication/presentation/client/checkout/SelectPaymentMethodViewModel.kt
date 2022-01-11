package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectPaymentMethodViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectPaymentMethodChannel = Channel<SelectPaymentMethodEvent>()
    val selectPaymentMethodEvent = _selectPaymentMethodChannel.receiveAsFlow()

    fun assignCheckedRadioButton(paymentMethod: PaymentMethod) = viewModelScope.launch {
        when (paymentMethod) {
            PaymentMethod.COD -> _selectPaymentMethodChannel.send(SelectPaymentMethodEvent.CheckCashOnDeliveryOption)
            PaymentMethod.CREDIT_DEBIT -> _selectPaymentMethodChannel.send(SelectPaymentMethodEvent.CheckCreditOption)
        }
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod) = viewModelScope.launch {
        async { preferencesManager.updatePaymentMethod(paymentMethod) }.await()
        _selectPaymentMethodChannel.send(SelectPaymentMethodEvent.NavigateBack)
    }

    sealed class SelectPaymentMethodEvent {
        object CheckCashOnDeliveryOption : SelectPaymentMethodEvent()
        object CheckCreditOption : SelectPaymentMethodEvent()
        object NavigateBack : SelectPaymentMethodEvent()
    }
}
