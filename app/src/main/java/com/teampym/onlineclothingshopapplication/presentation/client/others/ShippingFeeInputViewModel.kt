package com.teampym.onlineclothingshopapplication.presentation.client.others

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ShippingFeeInputViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private var _isSuccessful = MutableLiveData(false)
    val isSuccessful: LiveData<Boolean> get() = _isSuccessful

    private var userId = ""
    private var _userType = MutableLiveData("")

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userId = sessionPref.userId
        userInformationDao.get(sessionPref.userId)
    }

    companion object {
        private const val SHIPPING_FEE = "shipping_fee"
    }

    var shippingFee = state.get<BigDecimal>(SHIPPING_FEE) ?: (0).toBigDecimal()
        set(value) {
            field = value
            state.set(SHIPPING_FEE, value)
        }

    fun updateUserType(userType: String) {
        _userType.value = userType
    }

    fun submitSuggestedShippingFee(order: Order, shippingFee: BigDecimal) = appScope.launch {
        if (_userType.value != null) {
            _isSuccessful.value = orderRepository.updateOrderStatus(
                userId,
                _userType.value!!,
                order.id,
                Status.SHIPPED.name,
                null,
                shippingFee.toDouble()
            )
        }
    }
}
