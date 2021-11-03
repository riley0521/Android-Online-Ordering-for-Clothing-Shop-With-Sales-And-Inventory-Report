package com.teampym.onlineclothingshopapplication.presentation.client.products

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.ProductInventoryRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import kotlinx.coroutines.tasks.await
import java.io.IOException
import javax.inject.Inject

private const val DEFAULT_PAGE_INDEX = 1

class ProductPagingSource(
    private val queryProducts: Query,
    private val productImageRepository: ProductImageRepositoryImpl,
    private val productInventoryRepository: ProductInventoryRepositoryImpl,
    private val reviewRepository: ReviewRepositoryImpl
) : PagingSource<QuerySnapshot, Product>() {

    private val productCollectionRef = FirebaseFirestore.getInstance().collection(PRODUCTS_COLLECTION)

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Product>): QuerySnapshot? {
        TODO("Not yet implemented")
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
                if(product != null) {
                    val inventoryList = productInventoryRepository.getAll(document.id)

                    val productImageList = productImageRepository.getAll(document.id)

                    val reviewList = reviewRepository.getFive(document.id)

                    val productItem = product.copy(
                        id = document.id,
                        inventoryList = inventoryList,
                        productImageList = productImageList,
                        reviewList = reviewList
                    )
                    productList.add(productItem)
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