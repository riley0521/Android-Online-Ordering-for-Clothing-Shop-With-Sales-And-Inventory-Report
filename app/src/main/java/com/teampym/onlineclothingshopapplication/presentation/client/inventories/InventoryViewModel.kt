package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

    private val _inventoriesLiveData = MutableLiveData<List<Inventory>>()
    val inventories: LiveData<List<Inventory>> get() = _inventoriesLiveData

    private val productEventChannel = Channel<ProductEvent>()
    val productEvent = productEventChannel.receiveAsFlow()

    fun loadInventories(productId: String) = viewModelScope.launch {
        val inventoryDb = db.collection("Products")
            .document(productId)
            .collection("Inventories")
            .limit(10)
            .get()
            .await()

        val inventoryList = mutableListOf<Inventory>()
        for (inventory in inventoryDb.documents) {
            inventoryList.add(
                Inventory(
                    inventory.getString("size")!!,
                    inventory.getLong("stock")!!,
                    inventory.getLong("committed")!!,
                    inventory.getLong("sold")!!,
                    inventory.getLong("returned")!!,
                    inventory.getLong("restockLevel")!!,
                )
            )
        }

        _inventoriesLiveData.value = inventoryList
    }

    fun addToCart(product: Product, inventory: Inventory, userId: String) = viewModelScope.launch {
        // Create CartRepository and use online database instead of Room
    }

    sealed class ProductEvent {
        data class AddOrUpdateCart(val count: Long, val name: String, val size: String) : ProductEvent()
    }

}