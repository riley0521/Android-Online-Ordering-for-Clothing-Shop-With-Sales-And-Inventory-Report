package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    val statusQuery = MutableLiveData("")

    private val _orderListChannel = Channel<OrderListEvent>()
    val orderEvent = _orderListChannel.receiveAsFlow()

    private val _userSessionFlow = preferencesManager.preferencesFlow
    val userSession = _userSessionFlow.asLiveData()

    var orders = MutableLiveData<PagingData<Order>>()

    fun getOrders() {
        orders = combine(
            searchQuery.asFlow(),
            statusQuery.asFlow(),
            _userSessionFlow
        ) { search, status, user ->
            Triple(search, status, user)
        }.flatMapLatest { (search, status, user) ->
            val queryProducts = if (search.isEmpty()) {
                if (user.userType == UserType.CUSTOMER.name) {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .whereEqualTo("userId", user.userId)
                        .orderBy("dateOrdered", Query.Direction.DESCENDING)
                        .limit(30)
                } else {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .orderBy("dateOrdered", Query.Direction.ASCENDING)
                        .limit(30)
                }
            } else {
                if (user.userType == UserType.CUSTOMER.name) {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .whereEqualTo("userId", user.userId)
                        .orderBy("dateOrdered", Query.Direction.DESCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                } else {
                    db.collection(ORDERS_COLLECTION)
                        .whereEqualTo("status", status)
                        .orderBy("dateOrdered", Query.Direction.ASCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                }
            }

            orderRepository.getSome(queryProducts, user.userId, user.userType).flow.cachedIn(
                viewModelScope
            )
        }.asLiveData() as MutableLiveData<PagingData<Order>>
    }

    fun checkOrderIfCancellable(order: Order): Boolean {
        val dateOrdered = Calendar.getInstance()
        dateOrdered.timeInMillis = order.dateOrdered
        dateOrdered.timeZone = TimeZone.getTimeZone("GMT+8:00")

        // Check if today's day of year is == the day of year in dateOrdered variable
        return Calendar.getInstance()
            .get(Calendar.DAY_OF_YEAR) == dateOrdered.get(Calendar.DAY_OF_YEAR)
    }

    fun cancelOrder(
        order: Order
    ) = viewModelScope.launch {
        val res = orderRepository.cancelOrder(
            order.deliveryInformation.name,
            order.id
        )

        if (res) {
            _orderListChannel.send(
                OrderListEvent.ShowSuccessMessage(
                    "Cancelled Order Successfully!"
                )
            )
        } else {
            _orderListChannel.send(
                OrderListEvent.ShowErrorMessage(
                    "Cancelling order failed. Please try again."
                )
            )
        }
    }

    fun deliverOrder(order: Order) = viewModelScope.launch {
        val session = _userSessionFlow.first()
        val userInformation = userInformationDao.getCurrentUser(session.userId)

        userInformation?.let {
            val res = orderRepository.updateOrderStatus(
                username = "${userInformation.firstName} ${userInformation.lastName}",
                userId = userInformation.userId,
                userType = userInformation.userType,
                orderId = order.id,
                status = Status.DELIVERY.name,
                null,
                null
            )

            if (res) {
                _orderListChannel.send(
                    OrderListEvent.ShowSuccessMessage(
                        "Changed Order status to Delivery successfully!"
                    )
                )
            } else {
                _orderListChannel.send(
                    OrderListEvent.ShowErrorMessage(
                        "Changing Order status to Delivery failed. Please try again."
                    )
                )
            }
        }
    }

    fun completeOrder(order: Order) = viewModelScope.launch {
        val session = _userSessionFlow.first()
        val userInformation = userInformationDao.getCurrentUser(session.userId)

        userInformation?.let {
            val res = orderRepository.updateOrderStatus(
                username = "${userInformation.firstName} ${userInformation.lastName}",
                userId = userInformation.userId,
                userType = userInformation.userType,
                orderId = order.id,
                status = Status.COMPLETED.name,
                null,
                null
            )

            if (res) {
                _orderListChannel.send(
                    OrderListEvent.ShowSuccessMessage(
                        "Changed Order status to Completed successfully!"
                    )
                )
            } else {
                _orderListChannel.send(
                    OrderListEvent.ShowErrorMessage(
                        "Changing Order status to Completed failed. Please try again."
                    )
                )
            }
        }
    }

    fun onAdminCancelResult(
        result: String,
        order: Order
    ) = viewModelScope.launch {
        _orderListChannel.send(OrderListEvent.ShowSuccessMessage("$result - ${order.id}"))
    }

    fun onSuggestedShippingFeeResult(
        result: String,
        order: Order
    ) = viewModelScope.launch {
        _orderListChannel.send(OrderListEvent.ShowSuccessMessage("$result - ${order.id}"))
    }

    fun onAgreeToSfResult(
        result: String,
        order: Order
    ) = viewModelScope.launch {
        _orderListChannel.send(OrderListEvent.ShowSuccessMessage("$result - ${order.id}"))
    }

    sealed class OrderListEvent {
        data class ShowSuccessMessage(val msg: String) : OrderListEvent()
        data class ShowErrorMessage(val msg: String) : OrderListEvent()
    }
}
