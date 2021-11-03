package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.Product
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepositoryImpl,
    private val reviewRepository: ReviewRepositoryImpl
): ViewModel() {

    fun getProductById(productId: String): Product {
        var selectedProduct = Product()
        viewModelScope.launch {
            selectedProduct = productRepository.getOne(productId)
        }
        return selectedProduct
    }


    fun getAvgRate(productId: String): Double {
        var avgRate = 0.0
        viewModelScope.launch {
            avgRate = reviewRepository.getAvgRate(productId)
        }
        return avgRate
    }

}