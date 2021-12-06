package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository,
    private val preferencesManager: PreferencesManager,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val PRODUCT = "product"
    }

    var product = state.get<Product>(PRODUCT) ?: Product()
        set(value) {
            field = value
            state.set(PRODUCT, value)
        }

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }

    fun getProductById(productId: String) = viewModelScope.launch {
        productRepository.getOne(productId)?.let {
            val reviewList = async { reviewRepository.getFive(it.productId) }

            val prod = it
            prod.reviewList = reviewList.await()

            product = prod
        }
    }

    fun updateProduct(p: Product) {
        product = p
    }
}
