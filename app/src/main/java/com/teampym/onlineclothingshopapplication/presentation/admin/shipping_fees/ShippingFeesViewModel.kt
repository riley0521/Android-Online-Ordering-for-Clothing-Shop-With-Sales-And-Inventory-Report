package com.teampym.onlineclothingshopapplication.presentation.admin.shipping_fees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.ShippingFee
import com.teampym.onlineclothingshopapplication.data.repository.ShippingFeesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShippingFeesViewModel @Inject constructor(
    private val shippingFeesRepository: ShippingFeesRepository
) : ViewModel() {

    var shippingFee: ShippingFee = ShippingFee()

    private val _shippingFeesChannel = Channel<ShippingFeesEvent>()
    val shippingFeesEvent = _shippingFeesChannel.receiveAsFlow()

    fun loadShippingFees(onLoad: (ShippingFee) -> Unit) {
        viewModelScope.launch {
            shippingFeesRepository.get()?.let {
                onLoad.invoke(it)
            }
        }
    }

    fun onSubmitClicked() = viewModelScope.launch {
        if (isFormValid()) {
            val result = shippingFeesRepository.update(shippingFee)
            if (result) {
                _shippingFeesChannel.send(ShippingFeesEvent.ShowSuccessMessage("Shipping Fees updated successfully!"))
            } else {
                _shippingFeesChannel.send(ShippingFeesEvent.ShowErrorMessage("Updating Shipping Fees failed. Please try again."))
            }
        } else {
            _shippingFeesChannel.send(ShippingFeesEvent.ShowErrorMessage("Please fill the form."))
        }
    }

    private fun isFormValid(): Boolean {
        return shippingFee.metroManila > 0 &&
            shippingFee.mindanao > 0 &&
            shippingFee.northLuzon > 0 &&
            shippingFee.southLuzon > 0 &&
            shippingFee.visayas > 0
    }

    sealed class ShippingFeesEvent {
        data class ShowSuccessMessage(val msg: String) : ShippingFeesEvent()
        data class ShowErrorMessage(val msg: String) : ShippingFeesEvent()
    }
}
