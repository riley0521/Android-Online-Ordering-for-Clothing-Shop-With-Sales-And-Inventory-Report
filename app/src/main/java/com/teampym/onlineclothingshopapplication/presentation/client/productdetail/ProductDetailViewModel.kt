package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val preferencesManager: PreferencesManager,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val PRODUCT = "product"
    }

    var product: MutableLiveData<Product?> = state.getLiveData(PRODUCT, null)
        set(value) {
            field = value
            state.set(PRODUCT, value)
        }

    fun updateProduct(p: Product) {
        product.postValue(p)
    }

    val userSession = preferencesManager.preferencesFlow

    fun getProductById(productId: String) = viewModelScope.launch {
        val res = async { productRepository.getOne(productId) }.await()
        if (res != null) {
            updateProduct(res)
        }
    }
}
