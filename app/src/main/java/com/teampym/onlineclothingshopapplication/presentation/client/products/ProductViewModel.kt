package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepositoryImpl
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    private val _categoryQuery = MutableLiveData("")
    private val _flagQuery = MutableLiveData(Utils.productFlag)

    var productsFlow = combine(
        searchQuery.asFlow(),
        _flagQuery.asFlow()
    ) { search, flag ->
        Pair(search, flag)
    }.flatMapLatest { (search, flag) ->

        lateinit var queryProducts: Query
        val categoryId = _categoryQuery.value
        if (flag.isEmpty()) {
            queryProducts = if (search.isEmpty()) {
                db.collection(PRODUCTS_COLLECTION)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .limit(30)
            } else {
                db.collection(PRODUCTS_COLLECTION)
                    .whereEqualTo("categoryId", categoryId)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .startAt(search)
                    .endAt(search + '\uf8ff')
                    .limit(30)
            }
        } else {
            queryProducts = db.collection(PRODUCTS_COLLECTION)
                .whereEqualTo("categoryId", categoryId)
                .whereEqualTo("flag", flag)
                .limit(30)
        }

        productRepository.getSome(queryProducts).flow
    }

    fun updateCategory(categoryId: String) {
        _categoryQuery.value = categoryId
    }

    fun updateFlag(flag: String) = viewModelScope.launch {
        Utils.productFlag = flag
        _flagQuery.value = flag
    }
}