package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.repository.WishItemRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val wishItemRepository: WishItemRepository,
    private val wishItemDao: WishItemDao,
    private val preferencesManager: PreferencesManager,
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

    private val _isExisting = MutableLiveData(false)
    val isExisting: LiveData<Boolean> get() = _isExisting

    fun updateProduct(p: Product) {
        product.postValue(p)
    }

    private val _productDetailChannel = Channel<ProductDetailEvent>()
    val productDetailEvent = _productDetailChannel.receiveAsFlow()

    fun getUserSession() = preferencesManager.preferencesFlow.asLiveData()

    fun getProductById(productId: String) = viewModelScope.launch {
        val res = async { productRepository.getOne(productId) }.await()
        if (res != null) {
            updateProduct(res)
        } else {
            _productDetailChannel.send(ProductDetailEvent.NavigateBackProductNotFound)
        }
    }

    fun checkIfProductExistInWishList(productId: String) = viewModelScope.launch {
        val isWishItemExist = wishItemDao.checkIfExisting(productId)
        _isExisting.value = isWishItemExist != null
    }

    fun onAddOrRemoveToWishListClick(
        product: Product,
        userId: String
    ) = viewModelScope.launch {
        val existingWishListItem = wishItemDao.checkIfExisting(product.productId)

        if (existingWishListItem != null) {
            val result = wishItemRepository.remove(userId, product.productId)
            if (result) {
                // Remove from local db as well
                wishItemDao.delete(existingWishListItem.productId)

                _productDetailChannel.send(
                    ProductDetailEvent.ShowSuccessMessage("Product removed from wish list.")
                )
            } else {
                _productDetailChannel.send(
                    ProductDetailEvent.ShowErrorMessage("Removing product from wish list failed. Please try again.")
                )
            }
        } else {
            val result = wishItemRepository.insert(userId, product)
            if (result != null) {
                // Save to local db
                wishItemDao.insert(result)

                _productDetailChannel.send(
                    ProductDetailEvent.ShowSuccessMessage("Product added to wish list.")
                )
            } else {
                _productDetailChannel.send(
                    ProductDetailEvent.ShowErrorMessage("Adding product to wish list failed. Please try again.")
                )
            }
        }
    }

    sealed class ProductDetailEvent {
        data class ShowSuccessMessage(val msg: String) : ProductDetailEvent()
        data class ShowErrorMessage(val msg: String) : ProductDetailEvent()
        object NavigateBackProductNotFound : ProductDetailEvent()
    }
}
