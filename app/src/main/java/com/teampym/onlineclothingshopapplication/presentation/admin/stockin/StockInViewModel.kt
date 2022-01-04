package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockInViewModel @Inject constructor(
    private val inventoryRepository: ProductInventoryRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope,
) : ViewModel() {

    var productName = ""

    fun onSubmitClicked(productId: String, inventoryId: String, stockToAdd: Int) = appScope.launch {
        if (stockToAdd > 0) {
            val userSession = preferencesManager.preferencesFlow.first()
            val userInformation = userInformationDao.getCurrentUser(userSession.userId)

            inventoryRepository.addStock(
                username = "${userInformation?.firstName} ${userInformation?.lastName}",
                productName = productName,
                productId,
                inventoryId,
                stockToAdd.toLong()
            )
        }
    }
}
