package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.teampym.onlineclothingshopapplication.data.room.MOST_POPULAR
import com.teampym.onlineclothingshopapplication.data.room.NEWEST
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageRepository
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepository
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import kotlinx.coroutines.tasks.await
import java.io.IOException

private const val DEFAULT_PAGE_INDEX = 1

class ProductPagingSource(
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

            val productList = mutableListOf<Product>()
            for (document in currentPage.documents) {

                val product = document.toObject(Product::class.java)
                if (product != null) {
                    val inventoryList = productInventoryRepository.getAll(document.id)

                    val productImageList = productImageRepository.getAll(document.id)

                    val reviewList = reviewRepository.getFive(document.id)

                    val productItem = product.copy(
                        productId = document.id,
                        inventoryList = inventoryList,
                        productImageList = productImageList,
                        reviewList = reviewList
                    )
                    productList.add(productItem)
                }
            }

            if (sortOrder == SortOrder.BY_POPULARITY) {
                productList.sortByDescending { product ->
                    product.inventoryList.maxOf { it.sold }
                }

                if(productList.size > 3) {
                    productList[0].flag = MOST_POPULAR
                    productList[1].flag = MOST_POPULAR
                    productList[2].flag = MOST_POPULAR
                }
            } else if (sortOrder == SortOrder.BY_NEWEST) {
                productList.sortByDescending { product ->
                    product.inventoryList.maxOf { it.sold }
                }

                if(productList.size > 3) {
                    productList[0].flag = NEWEST
                    productList[1].flag = NEWEST
                    productList[2].flag = NEWEST
                }
            }

            LoadResult.Page(
                data = productList,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
