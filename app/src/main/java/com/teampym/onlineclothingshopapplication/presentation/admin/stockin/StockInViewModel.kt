package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockInViewModel @Inject constructor(
    private val inventoryRepository: ProductInventoryRepository,
    @ApplicationScope val appScope: CoroutineScope,
) : ViewModel() {

    fun onSubmitClicked(productId: String, inventoryId: String, stockToAdd: Int) = appScope.launch {
        if (stockToAdd > 0) {
            inventoryRepository.addStock(productId, inventoryId, stockToAdd.toLong())
        }
    }
}
