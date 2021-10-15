package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Utils
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepositoryImpl
) : ViewModel() {

    val searchQuery = MutableLiveData("")

    var productsFlow = searchQuery.asFlow().flatMapLatest { query ->

        lateinit var queryProducts: Query
        val categoryId = Utils.categoryId
        val productFlag = Utils.productFlag
        if (productFlag.isEmpty()) {
            queryProducts = if (query.isEmpty()) {
                db.collection("Products")
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(30)
            } else {
                db.collection("Products")
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .startAt(query)
                    .endAt(query + '\uf8ff')
                    .limit(30)
            }
        } else {
            queryProducts = db.collection("Products")
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("flag", productFlag)
                .limit(30)
        }

        productRepository.getProductsPagingSource(queryProducts).flow
    }

    fun updateFlag(flag: String) = viewModelScope.launch {
        Utils.productFlag = flag
    }



}