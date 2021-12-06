package com.teampym.onlineclothingshopapplication.presentation.client.others

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgreeToShippingFeeViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private var userId = ""

    private var _hasAgreed = MutableLiveData(false)
    val hasAgreed: LiveData<Boolean> get() = _hasAgreed

    private var _isCanceled = MutableLiveData(false)
    val isCanceled: LiveData<Boolean> get() = _isCanceled

    private val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userId = sessionPref.userId
        userInformationDao.get(sessionPref.userId)
    }

    fun agreeToSf(order: Order) = appScope.launch {
        _hasAgreed.value = orderRepository.agreeToShippingFee(
            userId,
            order.id
        )
    }

    fun cancelOrder(order: Order) = appScope.launch {
        _isCanceled.value = orderRepository.cancelOrder(
            order.deliveryInformation.name,
            order.id,
            true
        )
    }
}
