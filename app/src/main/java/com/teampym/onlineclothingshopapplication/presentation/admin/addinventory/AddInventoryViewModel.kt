package com.teampym.onlineclothingshopapplication.presentation.admin.addinventory

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddInventoryViewModel @Inject constructor(
    private val inventoryRepository: ProductInventoryRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val PRODUCT_ID = "product_id"
        private const val INVENTORY_SIZE = "inventory_size"
        private const val INVENTORY_STOCK = "inventory_stock"
        private const val INVENTORY_RESTOCK_LEVEL = "inventory_restock_level"
        private const val INVENTORY_AVAIL_SIZE_LIST = "inventory_avail_size_list"
    }

    var productId = state.get(PRODUCT_ID) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_ID, value)
        }

    var inventorySize = state.get(INVENTORY_SIZE) ?: ""
        set(value) {
            field = value
            state.set(INVENTORY_SIZE, value)
        }

    var inventoryStock = state.get<Int>(INVENTORY_STOCK) ?: 0
        set(value) {
            field = value
            state.set(INVENTORY_STOCK, value)
        }

    var inventoryRestockLevel = state.get<Int>(INVENTORY_RESTOCK_LEVEL) ?: 0
        set(value) {
            field = value
            state.set(INVENTORY_RESTOCK_LEVEL, value)
        }

    val availableInvList: MutableLiveData<List<String>> = state.getLiveData(
        INVENTORY_AVAIL_SIZE_LIST,
        listOf()
    )

    private val _addInventoryChannel = Channel<AddInventoryEvent>()
    val addInventoryEvent = _addInventoryChannel.receiveAsFlow()

    suspend fun updateAvailableSizeList(list: List<String>) {
        availableInvList.postValue(list)
    }

    private fun resetAllUiState() {
        inventorySize = ""
        inventoryStock = 0
        inventoryRestockLevel = 0
    }

    private fun isFormValid(): Boolean {
        return productId.isNotBlank() &&
            inventorySize.isNotBlank() &&
            inventoryStock > 0 &&
            inventoryRestockLevel < inventoryStock
    }

    fun onSubmitClicked(isAddingAnother: Boolean) = viewModelScope.launch {
        if (isFormValid()) {
            val newInv = Inventory(
                pid = productId,
                size = inventorySize,
                stock = inventoryStock.toLong(),
                restockLevel = inventoryRestockLevel.toLong()
            )
            val res = async { inventoryRepository.create(newInv) }.await()
            if (res != null) {
                resetAllUiState()
                if (isAddingAnother) {
                    _addInventoryChannel.send(
                        AddInventoryEvent.ShowSuccessMessageAndResetState(
                            "New Size Successfully Added!"
                        )
                    )
                } else {
                    _addInventoryChannel.send(
                        AddInventoryEvent.NavigateBackWithMessage(
                            "New Size Successfully Added!"
                        )
                    )
                }
            }
        } else {
            _addInventoryChannel.send(
                AddInventoryEvent.ShowErrorMessage(
                    "Please fill the form."
                )
            )
        }
    }

    fun onLoadSizesInitiated() = viewModelScope.launch {
        val invList = async { inventoryRepository.getAll(productId) }.await()
        val availableSizes = mutableListOf<String>()
        invList.forEach {
            availableSizes.add(it.size)
        }
        updateAvailableSizeList(availableSizes)
    }

    sealed class AddInventoryEvent {
        data class NavigateBackWithMessage(val msg: String) : AddInventoryEvent()
        data class ShowSuccessMessageAndResetState(val msg: String) : AddInventoryEvent()
        data class ShowErrorMessage(val msg: String) : AddInventoryEvent()
    }
}
