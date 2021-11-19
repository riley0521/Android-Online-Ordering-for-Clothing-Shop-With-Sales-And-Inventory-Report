package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl,
    private val cartDao: CartDao,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _inventoriesLiveData = MutableLiveData<List<Inventory>>()
    val inventories: LiveData<List<Inventory>> get() = _inventoriesLiveData

//    private val productEventChannel = Channel<ProductEvent>()
//    val productEvent = productEventChannel.receiveAsFlow()

    fun addToCart(userId: String, product: Product, inventory: Inventory) = applicationScope.launch {
        // Use online database instead of Room
        val newCartItem = Cart(
            userId = userId,
            subTotal = 0.0,
            product = product,
            inventory = inventory
        )

        if (cartRepository.insert(userId, newCartItem)) {
            cartDao.insert(newCartItem)
        }
    }

    sealed class ProductEvent {
        data class AddedToCart(val msg: String) : ProductEvent()
    }
}
