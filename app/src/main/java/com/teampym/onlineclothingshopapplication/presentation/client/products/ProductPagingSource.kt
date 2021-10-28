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
import kotlinx.coroutines.tasks.await
import java.io.IOException

private const val DEFAULT_PAGE_INDEX = 1

class ProductPagingSource(
    private val queryProducts: Query
) : PagingSource<QuerySnapshot, Product>() {

    private val productCollectionRef = FirebaseFirestore.getInstance().collection("Product")

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
                    val inventoriesQuery = productCollectionRef.document(document.id).collection("inventories").get().await()
                    val inventoryList = mutableListOf<Inventory>()
                    inventoriesQuery?.let {
                        for(doc in inventoriesQuery.documents) {
                            val inventory = doc.toObject(Inventory::class.java)
                            if(inventory != null) {
                                val i = inventory.copy(id = doc.id)
                                inventoryList.add(i)

                            }
                        }
                    }

                    val productImagesQuery = productCollectionRef.document(document.id).collection("productImages").get().await()
                    val productImageList = mutableListOf<ProductImage>()
                    productImagesQuery?.let {
                        for(doc in productImagesQuery.documents) {
                            val productImage = doc.toObject(ProductImage::class.java)
                            if(productImage != null) {
                                val p = productImage.copy(id = doc.id)
                                productImageList.add(p)

                            }
                        }
                    }

                    val productItem = product.copy(id = document.id, inventories = inventoryList, productImages = productImageList)
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