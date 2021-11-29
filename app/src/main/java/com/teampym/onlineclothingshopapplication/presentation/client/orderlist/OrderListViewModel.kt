package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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
            if (user.userType == UserType.CUSTOMER.name) {
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
            if (user.userType == UserType.CUSTOMER.name) {
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
}
