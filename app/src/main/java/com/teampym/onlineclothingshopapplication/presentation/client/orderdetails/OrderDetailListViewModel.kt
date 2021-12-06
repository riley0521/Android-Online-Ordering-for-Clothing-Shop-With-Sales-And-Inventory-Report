package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.models.Order
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OrderDetailListViewModel @Inject constructor(
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val ORDER = "order"
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
