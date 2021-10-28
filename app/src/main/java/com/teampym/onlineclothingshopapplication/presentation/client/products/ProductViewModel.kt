package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Utils
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageWithInventoryAndReviewRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductImageWithInventoryAndReviewRepositoryImpl
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    val categoryQuery = MutableLiveData("")
    private val flagQuery = MutableLiveData(Utils.productFlag)


    var productsFlow = combine(
        searchQuery.asFlow(),
        flagQuery.asFlow()
    ) { search, flag ->
        Pair(search, flag)
    }.flatMapLatest { (search, flag) ->

        lateinit var queryProducts: Query
        val categoryId = categoryQuery.value
        val productFlag = flag
        if (productFlag.isEmpty()) {
            queryProducts = if (search.isEmpty()) {
                db.collection("Products")
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(30)
            } else {
                db.collection("Products")
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .startAt(search)
                    .endAt(search + '\uf8ff')
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

    fun updateCategory(categoryId: String) = viewModelScope.launch {
        categoryQuery.value = categoryId
    }

    fun updateFlag(flag: String) = viewModelScope.launch {
        Utils.productFlag = flag
        flagQuery.value = flag
    }
}