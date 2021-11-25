package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.repository.WishItemRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val productRepository: ProductRepository,
    private val wishListRepository: WishItemRepository,
    private val userInformationDao: UserInformationDao,
    private val wishListDao: WishItemDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val searchQuery = MutableLiveData("")
    private val _categoryQuery = MutableLiveData("")

    private val _userWithWishList = MutableLiveData<UserWithWishList>()
    val userWithWishList: LiveData<UserWithWishList> get() = _userWithWishList

    private val _productChannel = Channel<ProductEvent>()
    val productEvent = _productChannel.receiveAsFlow()

    val productsFlow = combine(
        searchQuery.asFlow(),
        preferencesManager.preferencesFlow
    ) { search, sessionPref ->
        Pair(search, sessionPref)
    }.flatMapLatest { (search, sessionPref) ->
        val queryProducts: Query?
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
            _productChannel.send(ProductEvent.ShowMessage("Added product to wish list."))
        }
    }

    fun updateCategory(categoryId: String) {
        _categoryQuery.value = categoryId
    }

    fun updateSortOrder(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    sealed class ProductEvent {
        data class ShowMessage(val msg: String) : ProductEvent()
    }
}
