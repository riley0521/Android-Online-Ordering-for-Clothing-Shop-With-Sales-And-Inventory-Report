package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.WishItemRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.room.*
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepositoryImpl,
    private val wishListRepository: WishItemRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val wishListDao: WishItemDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    private val _categoryQuery = MutableLiveData("")

    private val _userWithWishList = MutableLiveData<UserWithWishList>()
    val userWithWishList: LiveData<UserWithWishList> get() = _userWithWishList

    var productsFlow = combine(
        searchQuery.asFlow(),
        preferencesManager.preferencesFlow
    ) { search, sessionPref ->
        Pair(search, sessionPref)
    }.flatMapLatest { (search, sessionPref) ->

        var queryProducts: Query?
        val categoryId = _categoryQuery.value

        queryProducts = when (sessionPref.sortOrder) {
            SortOrder.BY_NAME -> {
                if (search.isEmpty()) {
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
            }
            SortOrder.BY_NEWEST -> {
                if (search.isEmpty()) {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", categoryId)
                        .orderBy("dateAdded", Query.Direction.DESCENDING)
                        .limit(30)
                } else {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", categoryId)
                        .orderBy("dateAdded", Query.Direction.DESCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                }
            }
            SortOrder.BY_POPULARITY -> {
                if (search.isEmpty()) {
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
            }
        }

        _userWithWishList.value = userInformationDao.getUserWithWishList().first {
            it.user.userId == sessionPref.userId
        }

        productRepository.getSome(queryProducts, sessionPref.sortOrder).flow
    }

    fun addToWishList(userId: String, product: Product) = viewModelScope.launch {
        wishListRepository.insert(userId, product)?.let {
            wishListDao.insert(it)
        }
    }

    fun updateCategory(categoryId: String) {
        _categoryQuery.value = categoryId
    }

    fun updateSortOrder(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }
}
