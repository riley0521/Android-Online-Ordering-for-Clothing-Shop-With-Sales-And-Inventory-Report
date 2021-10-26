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
import java.math.BigDecimal

private const val DEFAULT_PAGE_INDEX = 1

class ProductPagingSource(
    private val queryProducts: Query
) : PagingSource<QuerySnapshot, Product>() {

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
            for (product in currentPage.documents) {

                val productCollectionRef = FirebaseFirestore.getInstance().collection("Products")

                // get all inventories
                val inventoryQuery = productCollectionRef.document(product.id).collection("inventories").get().await()
                val inventoryList = mutableListOf<Inventory>()
                for(inventory in inventoryQuery) {
                    inventoryList.add(
                        Inventory(
                            id = inventory.id,
                            productId = inventory["productId"].toString(),
                            size = inventory["size"].toString(),
                            stock = inventory["stock"].toString().toLong() - inventory["committed"].toString().toLong(),
                            committed = inventory["committed"].toString().toLong(),
                            sold = inventory["sold"].toString().toLong(),
                            returned = inventory["returned"].toString().toLong(),
                            restockLevel = inventory["restockLevel"].toString().toLong()
                        )
                    )
                }

                // get all productImages
                val productImagesQuery = productCollectionRef.document(product.id).collection("productImages").get().await()
                val productImageList = mutableListOf<ProductImage>()
                for(productImage in productImagesQuery) {
                    productImageList.add(
                        ProductImage(
                            id = productImage.id,
                            productId = productImage["productId"].toString(),
                            imageUrl = productImage["imageUrl"].toString()
                        )
                    )
                }

                productList.add(
                    Product(
                        id = product.id,
                        categoryId = product["categoryId"].toString(),
                        name = product["name"].toString(),
                        description = product["description"].toString(),
                        imageUrl = product["imageUrl"].toString(),
                        price = product["price"].toString().toBigDecimal(),
                        flag = product["flag"].toString().replace("_", " "),
                        inventories = inventoryList,
                        productImages = productImageList
                    )
                )
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