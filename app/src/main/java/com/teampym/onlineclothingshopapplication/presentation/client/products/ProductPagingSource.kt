package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageRepository
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import com.teampym.onlineclothingshopapplication.data.room.MOST_POPULAR
import com.teampym.onlineclothingshopapplication.data.room.NEWEST
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.util.UserType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ProductPagingSource(
    private val user: UserWithWishList?,
    private val queryProducts: Query,
    private val sortOrder: SortOrder,
    private val productImageRepository: ProductImageRepository,
    private val productInventoryRepository: ProductInventoryRepository,
    private val reviewRepository: ReviewRepository
) : PagingSource<QuerySnapshot, Product>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Product>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Product> {
        return try {

            val currentPage = params.key ?: queryProducts
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryProducts.startAfter(lastVisibleItem).get().await()
            }

            var productList = mutableListOf<Product>()
            for (document in currentPage.documents) {

                val product = document.toObject<Product>()!!.copy(productId = document.id)
                CoroutineScope(Dispatchers.IO).launch {
                    val inventoryList = async { productInventoryRepository.getAll(document.id) }

                    val productImageList = async { productImageRepository.getAll(document.id) }

                    val reviewList = async { reviewRepository.getFive(document.id) }

                    product.inventoryList = inventoryList.await()
                    product.productImageList = productImageList.await()
                    product.reviewList = reviewList.await()
                }.join()
                productList.add(product)
            }

            productList.forEach {
                it.inventoryList.forEach {
                    Log.d("ProductPagingSource", it.toString())
                }
            }

            var finalProductList = mutableListOf<Product>()
            return if (user?.user != null && user.user.userType == UserType.ADMIN.name) {
                // Each Product has 1 inventory and it will repeat every item with different sizes.
                for (item in productList) {
                    for (inv in item.inventoryList) {
                        val newP = Product(
                            item.categoryId,
                            item.name,
                            item.description,
                            item.fileName,
                            item.imageUrl,
                            item.price,
                            item.productId,
                            type = item.type,
                            dateAdded = item.dateAdded,
                            dateModified = item.dateModified,
                            totalRate = item.totalRate,
                            numberOfReviews = item.numberOfReviews
                        )
                        newP.productImageList = item.productImageList
                        newP.reviewList = item.reviewList

                        newP.fastTrack = inv.sold
                        newP.inventoryList = listOf(inv)
                        finalProductList.add(newP)
                    }
                }

                // Sort by A - Z
                val nameComparator = compareBy<Product> { it.name }
                finalProductList = finalProductList
                    .sortedWith(
                        // Then sort by most sold size.
                        nameComparator.thenByDescending { it.fastTrack }
                    ).toMutableList()

                LoadResult.Page(
                    data = finalProductList,
                    prevKey = null,
                    nextKey = nextPage
                )
            } else {
                if (sortOrder == SortOrder.BY_POPULARITY) {
                    productList.sortByDescending { product ->
                        product.inventoryList.maxOf { it.sold }
                    }

                    if (productList.size > 5) {
                        productList[0].flag = MOST_POPULAR
                        productList[1].flag = MOST_POPULAR
                        productList[2].flag = MOST_POPULAR
                        productList[3].flag = MOST_POPULAR
                        productList[4].flag = MOST_POPULAR
                    }
                } else if (sortOrder == SortOrder.BY_NEWEST) {
                    productList.sortByDescending { product ->
                        product.inventoryList.maxOf { it.sold }
                    }

                    if (productList.size > 5) {
                        productList[0].flag = NEWEST
                        productList[1].flag = NEWEST
                        productList[2].flag = NEWEST
                        productList[3].flag = NEWEST
                        productList[4].flag = NEWEST
                    }
                }

                // When the admin adds a new product
                // It does not have an inventory automatically
                // So this line will filter all product that have at least 1 size.
                productList = productList.filter { it.inventoryList.isNotEmpty() }.toMutableList()

                // TODO("Map list here to modify if the user has wish listed an item instead doing it in the UI")

                LoadResult.Page(
                    data = productList,
                    prevKey = null,
                    nextKey = nextPage
                )
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
