package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
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
import kotlinx.coroutines.async
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
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val ADD_MENU_VISIBLE = "add_menu"
        private const val CART_MENU_VISIBLE = "cart_menu"
        private const val SORT_MENU_VISIBLE = "sort_menu"
        private const val CATEGORY_ID = "category_id"
    }

    val isAddMenuVisible: MutableLiveData<Boolean> =
        state.getLiveData(ADD_MENU_VISIBLE, false)

    val isCartMenuVisible: MutableLiveData<Boolean> =
        state.getLiveData(CART_MENU_VISIBLE, false)

    val isSortMenuVisible: MutableLiveData<Boolean> =
        state.getLiveData(SORT_MENU_VISIBLE, false)

    val categoryId: MutableLiveData<String> = state.getLiveData(CATEGORY_ID, "")

    fun updateAddMenu(isVisible: Boolean) = viewModelScope.launch {
        isAddMenuVisible.postValue(isVisible)
    }

    fun updateCartMenu(isVisible: Boolean) {
        isCartMenuVisible.postValue(isVisible)
    }

    fun updateSortMenu(isVisible: Boolean) {
        isSortMenuVisible.postValue(isVisible)
    }

    fun updateCategoryId(id: String) {
        categoryId.postValue(id)
    }

    override fun onCleared() {
        super.onCleared()
        state.set(ADD_MENU_VISIBLE, isAddMenuVisible.value)
        state.set(CART_MENU_VISIBLE, isCartMenuVisible.value)
        state.set(SORT_MENU_VISIBLE, isSortMenuVisible.value)
    }

    val searchQuery = MutableLiveData("")

    private val _userWithWishList = MutableLiveData<UserWithWishList?>()
    val userWithWishList: LiveData<UserWithWishList?> get() = _userWithWishList

    private val _productChannel = Channel<ProductEvent>()
    val productEvent = _productChannel.receiveAsFlow()

    val productsFlow = combine(
        categoryId.asFlow(),
        searchQuery.asFlow(),
        preferencesManager.preferencesFlow
    ) { categoryId, search, sessionPref ->
        Triple(categoryId, search, sessionPref)
    }.flatMapLatest { (categoryId, search, sessionPref) ->

        val queryProducts = when (sessionPref.sortOrder) {
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
            // This is not necessary but when you remove this, the compiler
            // will say it's exhaustive
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

        val userWithWishList = userInformationDao.getUserWithWishList().firstOrNull {
            it.user?.userId == sessionPref.userId
        }

        _userWithWishList.value = userWithWishList

        productRepository.getSome(
            userWithWishList,
            queryProducts,
            sessionPref.sortOrder
        ).flow.cachedIn(viewModelScope)
    }

    fun addOrRemoveToWishList(
        userId: String,
        product: Product,
        currentPagingData: PagingData<Product>,
        event: ProductFragment.ProductUpdateRemove,
        isWishListed: Boolean
    ) = viewModelScope.launch {
        val res = async { wishListRepository.insert(userId, product) }.await()
        if (res != null) {
            async { wishListDao.insert(res) }.await()

            when (event) {
                is ProductFragment.ProductUpdateRemove.Edit -> {
                    currentPagingData.map {
                        if (product.productId == it.productId) {
                            it.isWishListedByUser = isWishListed
                            return@map it
                        } else {
                            return@map it
                        }
                    }
                }
                is ProductFragment.ProductUpdateRemove.Remove -> {
                    // The code will be provided in onDeleteProductClicked() method
                }
            }

            _productChannel.send(
                ProductEvent.ShowAddedToWishListSuccessMessage(
                    "Added product to wish list.",
                    currentPagingData
                )
            )
        }
    }

    fun onDeleteProductClicked(
        product: Product,
        currentPagingData: PagingData<Product>,
        event: ProductFragment.ProductUpdateRemove
    ) = viewModelScope.launch {
        val res = productRepository.delete(product.productId)
        if (res) {

            when (event) {
                is ProductFragment.ProductUpdateRemove.Remove -> {
                    currentPagingData.filter {
                        product.productId != it.productId
                    }
                }
                is ProductFragment.ProductUpdateRemove.Edit -> {
                    // Nothing to do here...
                }
            }

            _productChannel.send(ProductEvent.ShowSuccessMessage("Deleted product successfully!", currentPagingData))
        } else {
            _productChannel.send(ProductEvent.ShowErrorMessage("Deleting product failed. Please wait for a while."))
        }
    }

    fun updateSortOrder(sortOrder: SortOrder) = viewModelScope.launch {
        preferencesManager.updateSortOrder(sortOrder)
    }

    sealed class ProductEvent {
        data class ShowAddedToWishListSuccessMessage(
            val msg: String,
            val currentPagingData: PagingData<Product>
        ) : ProductEvent()

        data class ShowSuccessMessage(val msg: String, val currentPagingData: PagingData<Product>) : ProductEvent()
        data class ShowErrorMessage(val msg: String) : ProductEvent()
    }
}
