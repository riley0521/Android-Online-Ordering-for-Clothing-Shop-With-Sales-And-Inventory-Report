package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
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

    val userSession = preferencesManager.preferencesFlow

    val userWishList = MutableLiveData<UserWithWishList?>(null)

    val searchQuery = MutableLiveData("")

    private val _productChannel = Channel<ProductEvent>()
    val productEvent = _productChannel.receiveAsFlow()

    val productsFlow = combine(
        searchQuery.asFlow(),
        preferencesManager.preferencesFlow
    ) { search, sessionPref ->
        Pair(search, sessionPref)
    }.flatMapLatest { (search, sessionPref) ->

        val queryProducts = when (sessionPref.sortOrder) {
            SortOrder.BY_NAME -> {
                if (search.isEmpty()) {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("name", Query.Direction.ASCENDING)
                        .limit(30)
                } else {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("name", Query.Direction.ASCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                }
            }
            SortOrder.BY_NEWEST -> {
                if (search.isEmpty()) {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("dateAdded", Query.Direction.DESCENDING)
                        .limit(30)
                } else {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("dateAdded", Query.Direction.DESCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                }
            }
            // This is not necessary but when you remove this, the compiler
            // will say it's exhaustive
            SortOrder.BY_POPULARITY -> {
                if (search.isEmpty()) {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("name", Query.Direction.ASCENDING)
                        .limit(30)
                } else {
                    db.collection(PRODUCTS_COLLECTION)
                        .whereEqualTo("categoryId", sessionPref.categoryId)
                        .orderBy("name", Query.Direction.ASCENDING)
                        .startAt(search)
                        .endAt(search + '\uf8ff')
                        .limit(30)
                }
            }
        }

        val userWithWishList = userInformationDao.getUserWithWishList().firstOrNull {
            it.user?.userId == sessionPref.userId
        }

        productRepository.getSome(
            userWithWishList,
            queryProducts,
            sessionPref.sortOrder
        ).flow.cachedIn(viewModelScope)
    }

    fun fetchWishList(userId: String) = viewModelScope.launch {
        userWishList.postValue(userInformationDao.getUserWithWishList().firstOrNull { it.user?.userId == userId })
    }

//    fun addOrRemoveToWishList(
//        userId: String,
//        product: Product,
//        currentPagingData: PagingData<Product>,
//        event: ProductFragment.ProductUpdateRemove,
//        isWishListed: Boolean
//    ) = viewModelScope.launch {
//        val res = async { wishListRepository.insert(userId, product) }.await()
//        if (res != null) {
//            async { wishListDao.insert(res) }.await()
//
//            when (event) {
//                is ProductFragment.ProductUpdateRemove.Edit -> {
//                    currentPagingData.map {
//                        if (product.productId == it.productId) {
//                            it.isWishListedByUser = isWishListed
//                            return@map it
//                        } else {
//                            return@map it
//                        }
//                    }
//                }
//                is ProductFragment.ProductUpdateRemove.Remove -> {
//                    // The code will be provided in onDeleteProductClicked() method
//                }
//            }
//
//            _productChannel.send(
//                ProductEvent.ShowAddedToWishListSuccessMessage(
//                    "Added product to wish list.",
//                    currentPagingData
//                )
//            )
//        }
//    }

    fun onDeleteProductClicked(
        product: Product
    ) = viewModelScope.launch {
        val res = productRepository.delete(product.productId)
        if (res) {
            _productChannel.send(ProductEvent.ShowSuccessMessage("Deleted product successfully!"))
        } else {
            _productChannel.send(ProductEvent.ShowErrorMessage("Deleting product failed. Please wait for a while."))
        }
    }

    fun updateSortOrder(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    sealed class ProductEvent {
        data class ShowSuccessMessage(val msg: String) : ProductEvent()
        data class ShowErrorMessage(val msg: String) : ProductEvent()
    }
}
