package com.teampym.onlineclothingshopapplication.presentation.client.return_items

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.OrderDetailRepository
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.ORDER_DETAILS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReturnItemsSort {
    BY_REQUESTED,
    BY_RETURNED
}

@HiltViewModel
class ReturnItemsViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val orderRepository: OrderRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val sort = MutableLiveData(ReturnItemsSort.BY_REQUESTED)

    val userInfo = MutableLiveData<UserInformation>()

    var userType = ""

    init {
        viewModelScope.launch {
            val session = preferencesManager.preferencesFlow.first()
            userType = session.userType
            userInfo.postValue(userInformationDao.getCurrentUser(session.userId))
        }
    }

    private val _returnItemsChannel = Channel<ReturnItemsEvent>()
    val returnItemsEvent = _returnItemsChannel.receiveAsFlow()

    val orderItems = combine(
        sort.asFlow(),
        preferencesManager.preferencesFlow
    ) { sort, session ->
        Pair(sort, session)
    }.flatMapLatest { (sort, session) ->
        val queryOrderItems = when (sort) {
            ReturnItemsSort.BY_REQUESTED -> {
                if (session.userType == UserType.ADMIN.name) {
                    db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                        .whereEqualTo("requestedToReturn", true)
                        .whereEqualTo("returned", false)
                        .limit(30)
                } else {
                    db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                        .whereEqualTo("requestedToReturn", true)
                        .whereEqualTo("returned", false)
                        .whereEqualTo("userId", session.userId)
                        .limit(30)
                }
            }
            ReturnItemsSort.BY_RETURNED -> {
                if (session.userType == UserType.ADMIN.name) {
                    db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                        .whereEqualTo("requestedToReturn", true)
                        .whereEqualTo("returned", true)
                        .limit(30)
                } else {
                    db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                        .whereEqualTo("requestedToReturn", true)
                        .whereEqualTo("returned", true)
                        .whereEqualTo("userId", session.userId)
                        .limit(30)
                }
            }
            else -> db.collectionGroup(ORDER_DETAILS_SUB_COLLECTION)
                .whereEqualTo("requestedToReturn", true)
                .whereEqualTo("returned", false)
                .limit(30)
        }

        orderDetailRepository.getAllRequestedToReturnItems(queryOrderItems).flow.cachedIn(
            viewModelScope
        )
    }

    fun confirmOrder(orderItem: OrderDetail) = viewModelScope.launch {
        val res = orderRepository.confirmReturnedItem(orderItem)
        if (res) {
            _returnItemsChannel.send(ReturnItemsEvent.ShowSuccessMessage("Confirmed return item successfully!"))
        } else {
            _returnItemsChannel.send(ReturnItemsEvent.ShowErrorMessage("Confirmed return item failed. Please try again."))
        }
    }

    sealed class ReturnItemsEvent {
        data class ShowSuccessMessage(val msg: String) : ReturnItemsEvent()
        data class ShowErrorMessage(val msg: String) : ReturnItemsEvent()
    }
}
