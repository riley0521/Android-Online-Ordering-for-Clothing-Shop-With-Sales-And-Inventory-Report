package com.teampym.onlineclothingshopapplication.presentation.client.others

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ShippingFeeInputViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle,
    preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val SHIPPING_FEE = "shipping_fee"
    }

    private var userId = ""
    private var _userType = MutableLiveData("")

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userId = sessionPref.userId
        userInformationDao.get(sessionPref.userId)
    }

    var shippingFee = state.get<BigDecimal>(SHIPPING_FEE) ?: (0).toBigDecimal()
        set(value) {
            field = value
            state.set(SHIPPING_FEE, value)
        }

    private val _othersChannel = Channel<OtherDialogFragmentEvent>()
    val otherDialogEvent = _othersChannel.receiveAsFlow()

    fun updateUserType(userType: String) {
        _userType.value = userType
    }

    fun submitSuggestedShippingFee(order: Order, shippingFee: BigDecimal) = viewModelScope.launch {
        if (_userType.value != null) {
            val userInformation = userFlow.first()

            orderRepository.updateOrderStatus(
                username = "${userInformation?.firstName} ${userInformation?.lastName}",
                userId,
                _userType.value!!,
                order.id,
                Status.SHIPPED.name,
                null,
                shippingFee.toDouble()
            )

            _othersChannel.send(OtherDialogFragmentEvent.NavigateBack)
        }
    }
}
