package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.network.GoogleSheetService
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject

@HiltViewModel
class StockInViewModel @Inject constructor(
    private val inventoryRepository: ProductInventoryRepository,
    private val userInformationDao: UserInformationDao,
    private val googleSheetService: GoogleSheetService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var product = MutableLiveData<Product>()

    private val _stockInChannel = Channel<StockInEvent>()
    val stockInEvent = _stockInChannel.receiveAsFlow()

    fun fetchAllSizes(p: Product) = viewModelScope.launch {
        p.inventoryList = inventoryRepository.getAll(p.productId)
        product.postValue(p)
    }

    fun onSubmitClicked(productId: String, inventoryId: String, stockToAdd: Int) =
        viewModelScope.launch {
            if (stockToAdd > 0) {
                val userSession = preferencesManager.preferencesFlow.first()
                val userInformation = userInformationDao.getCurrentUser(userSession.userId)

                val inv = inventoryRepository.addStock(
                    username = "${userInformation?.firstName} ${userInformation?.lastName}",
                    productName = product.value!!.name,
                    productId,
                    inventoryId,
                    stockToAdd.toLong()
                )
                if (inv != null) {

                    val calendarDate = Calendar.getInstance()
                    calendarDate.timeInMillis = Utils.getTimeInMillisUTC()
                    val formattedDate = SimpleDateFormat("MM/dd/yyyy").format(calendarDate.time)

                    viewModelScope.launch {
                        googleSheetService.insertInventory(
                            date = formattedDate,
                            productId = inv.pid,
                            inventoryId = inv.inventoryId,
                            productName = inv.productName,
                            size = inv.size,
                            stock = inv.stock.toString(),
                            committed = inv.committed.toString(),
                            sold = inv.sold.toString(),
                            returned = inv.returned.toString(),
                            weightInKg = inv.weightInKg.toString() + " Kilogram"
                        )
                    }

                    _stockInChannel.send(StockInEvent.NavigateBackWithResult(true))
                } else {
                    _stockInChannel.send(StockInEvent.ShowErrorMessage("Adding stock failed. Please try again."))
                }
            }
        }

    sealed class StockInEvent {
        data class NavigateBackWithResult(val isSuccess: Boolean) : StockInEvent()
        data class ShowErrorMessage(val msg: String) : StockInEvent()
    }
}
