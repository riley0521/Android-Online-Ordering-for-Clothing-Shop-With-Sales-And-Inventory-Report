package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class OrderDetailListViewModel @Inject constructor(
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ORDER = "order"
    }

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }

    var order = state.get<Order>(ORDER) ?: Order()
        set(value) {
            field = value
            state.set(ORDER, value)
        }

    fun updateOrder(o: Order) {
        order = o
    }
}
