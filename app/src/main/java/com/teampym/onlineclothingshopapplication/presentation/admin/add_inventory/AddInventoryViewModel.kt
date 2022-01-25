package com.teampym.onlineclothingshopapplication.presentation.admin.add_inventory

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.network.GoogleSheetService
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AddInventoryViewModel @Inject constructor(
    private val inventoryRepository: ProductInventoryRepository,
    private val userInformationDao: UserInformationDao,
    private val googleSheetService: GoogleSheetService,
    preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val PRODUCT_ID = "product_id"
        private const val PRODUCT_NAME = "product_name"
        private const val INVENTORY_SIZE = "inventory_size"
        private const val INVENTORY_STOCK = "inventory_stock"
        private const val WEIGHT_IN_KG = "weight_in_kg"
        private const val INVENTORY_AVAIL_SIZE_LIST = "inventory_avail_size_list"
    }

    var productId = state.get(PRODUCT_ID) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_ID, value)
        }

    var productName = state.get(PRODUCT_NAME) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_NAME, value)
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

    var weightInKg = state.get<Double>(WEIGHT_IN_KG) ?: 0.0
        set(value) {
            field = value
            state.set(WEIGHT_IN_KG, value)
        }

    val availableInvList: MutableLiveData<List<String>> = state.getLiveData(
        INVENTORY_AVAIL_SIZE_LIST,
        listOf()
    )

    private val _addInventoryChannel = Channel<AddInventoryEvent>()
    val addInventoryEvent = _addInventoryChannel.receiveAsFlow()

    private fun updateAvailableSizeList(list: List<String>) {
        availableInvList.postValue(list)
    }

    private fun resetAllUiState() {
        inventorySize = ""
        inventoryStock = 0
    }

    private fun isFormValid(): Boolean {
        return productId.isNotBlank() &&
            inventorySize.isNotBlank() &&
            inventoryStock > 0
    }

    val session = preferencesManager.preferencesFlow

    fun onSubmitClicked(isAddingAnother: Boolean) = viewModelScope.launch {
        val userSession = session.first()
        val userInformation = userInformationDao.getCurrentUser(userSession.userId)

        if (isFormValid()) {
            val newInv = Inventory(
                pid = productId,
                size = inventorySize,
                stock = inventoryStock.toLong(),
                weightInKg = if (weightInKg > 0.0) weightInKg else 0.20
            )
            val res = inventoryRepository.create(
                username = "${userInformation?.firstName} ${userInformation?.lastName}",
                productName = productName,
                newInv
            )
            if (res != null) {

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = Utils.getTimeInMillisUTC()
                val formattedDate = SimpleDateFormat("MM/dd/yyyy").format(calendarDate.time)

                googleSheetService.insertInventory(
                    date = formattedDate,
                    productId = productId,
                    inventoryId = res.inventoryId,
                    productName = productName,
                    size = inventorySize,
                    stock = inventoryStock.toString(),
                    committed = "0",
                    sold = "0",
                    returned = "0",
                    weightInKg = if (weightInKg > 0.0) "$weightInKg Kilogram" else "0.20 Kilogram"
                )

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
        val invList = inventoryRepository.getAll(productId)
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
