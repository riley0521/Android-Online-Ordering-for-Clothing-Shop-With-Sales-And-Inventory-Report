package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import com.teampym.onlineclothingshopapplication.data.repository.CartRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val cartRepository: CartRepositoryImpl,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _inventoriesLiveData = MutableLiveData<List<Inventory>>()
    val inventories: LiveData<List<Inventory>> get() = _inventoriesLiveData

    private val productEventChannel = Channel<ProductEvent>()
    val productEvent = productEventChannel.receiveAsFlow()

    fun addToCart(userId: String, product: Product, inventory: Inventory) = applicationScope.launch {
        // Use online database instead of Room
        // Check if the user is logged in
        val isAddedToCart = cartRepository.addToCart(userId, product, inventory)
        productEventChannel.send(ProductEvent.AddedToCart(isAddedToCart))
    }

    sealed class ProductEvent {
        data class AddedToCart(val msg: String) : ProductEvent()
    }

}