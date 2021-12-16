package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.*
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

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }

    val ordersFlow = combine(
        searchQuery.asFlow(),
        statusQuery.asFlow(),
        userFlow
    ) { search, status, user ->
        Triple(search, status, user)
    }.flatMapLatest { (search, status, user) ->
        val queryProducts = if (search.isEmpty()) {
            if (user!!.userType == UserType.CUSTOMER.name) {
                db.collection(ORDERS_COLLECTION)
                    .whereEqualTo("status", status)
                    .orderBy("dateOrdered", Query.Direction.DESCENDING)
                    .limit(30)
            } else {
                db.collection(ORDERS_COLLECTION)
                    .whereEqualTo("status", status)
                    .orderBy("dateOrdered", Query.Direction.ASCENDING)
                    .limit(30)
            }
        } else {
            if (user!!.userType == UserType.CUSTOMER.name) {
                db.collection(ORDERS_COLLECTION)
                    .whereEqualTo("status", status)
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
        order: Order,
        currentPagingData: PagingData<Order>,
        event: OrderListFragment.OrderRemoveEvent
    ) = viewModelScope.launch {

        val res = async {
            orderRepository.cancelOrder(
                order.deliveryInformation.name,
                order.id,
                false
            )
        }.await()

        if (res) {

            when (event) {
                is OrderListFragment.OrderRemoveEvent.Remove -> {
                    currentPagingData.filter {
                        order.id != it.id
                    }
                }
            }

            _orderListChannel.send(
                OrderListEvent.ShowMessage(
                    "Cancelled Order Successfully!",
                    currentPagingData
                )
            )
        }
    }

    fun onAdminCancelResult(
        result: String,
        order: Order,
        currentPagingData: PagingData<Order>,
        event: OrderListFragment.OrderRemoveEvent
    ) = viewModelScope.launch {

        when (event) {
            is OrderListFragment.OrderRemoveEvent.Remove -> {
                currentPagingData.filter {
                    order.id != it.id
                }
            }
        }

        _orderListChannel.send(OrderListEvent.ShowMessage(result, currentPagingData))
    }

    fun onSuggestedShippingFeeResult(
        result: String,
        order: Order,
        currentPagingData: PagingData<Order>,
        event: OrderListFragment.OrderRemoveEvent
    ) = viewModelScope.launch {

        when (event) {
            is OrderListFragment.OrderRemoveEvent.Remove -> {
                currentPagingData.filter {
                    order.id != it.id
                }
            }
        }

        _orderListChannel.send(OrderListEvent.ShowMessage(result, currentPagingData))
    }

    fun onAgreeToSfResult(
        result: String,
        order: Order,
        currentPagingData: PagingData<Order>,
        event: OrderListFragment.OrderRemoveEvent
    ) = viewModelScope.launch {

        when (event) {
            is OrderListFragment.OrderRemoveEvent.Remove -> {
                currentPagingData.filter {
                    order.id != it.id
                }
            }
        }

        _orderListChannel.send(OrderListEvent.ShowMessage(result, currentPagingData))
    }

    sealed class OrderListEvent {
        data class ShowMessage(val msg: String, val currentPagingData: PagingData<Order>) :
            OrderListEvent()
    }
}
