package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepository,
    private val reviewRepository: ReviewRepository
): ViewModel() {

    fun getProductById(productId: String): Product {
        var selectedProduct = Product()
        viewModelScope.launch {
            productRepository.getOne(productId)?.let {
                selectedProduct = it
            }
        }
        return selectedProduct
    }

}