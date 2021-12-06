package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.CartRepository
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
    private val cartRepository: CartRepository,
    private val cartDao: CartDao,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _inventoriesLiveData = MutableLiveData<List<Inventory>>()
    val inventories: LiveData<List<Inventory>> get() = _inventoriesLiveData

    fun addToCart(userId: String, product: Product, inventory: Inventory) = appScope.launch {
        // Use online database instead of Room
        val newCartItem = Cart(
            userId = userId,
            subTotal = 0.0,
            // Cart ID Should be == inventory ID
            id = inventory.inventoryId,
            product = product,
            inventory = inventory
        )

        // Set subTotal
        newCartItem.subTotal = newCartItem.calculatedTotalPrice.toDouble()

        cartRepository.insert(userId, newCartItem)
        cartDao.insert(newCartItem)
    }
}
