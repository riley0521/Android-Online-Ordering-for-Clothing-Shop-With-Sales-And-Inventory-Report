package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.OrderDetailRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val userInformationDao: UserInformationDao,
    preferencesManager: PreferencesManager,
    state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ORDER = "order"
        const val USER_INFO = "user_info"
    }

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }

    var order = state.getLiveData<Order>(ORDER, null)
    var userInfo = state.getLiveData<UserInformation>(USER_INFO, null)

    fun updateOrder(o: Order) {
        order.postValue(o)
    }

    fun fetchOrderWithDetailsById(
        orderId: String,
        userId: String
    ) = viewModelScope.launch {

        val order = orderRepository.getOne(
            orderId
        )

        order.orderDetailList = orderDetailRepository.getByOrderId(
            orderId,
            UserType.ADMIN.name,
            userId
        )

        updateOrder(order)
    }

    suspend fun checkItemIfCanReturn(item: OrderDetail): Boolean {
        return orderDetailRepository.canReturnItem(item)
    }

    suspend fun checkItemIfCanAddReview(item: OrderDetail): Boolean {
        return orderDetailRepository.canAddReview(item)
    }
}
