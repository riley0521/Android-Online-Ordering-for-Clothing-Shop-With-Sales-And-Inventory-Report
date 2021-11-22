package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val productImageRepository: ProductImageRepositoryImpl,
    private val productInventoryRepository: ProductInventoryRepositoryImpl,
    private val reviewRepository: ReviewRepositoryImpl,
    private val accountRepository: AccountRepositoryImpl
) {

    // READ Operation
    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    fun getSome(queryProducts: Query, sortOrder: SortOrder) =
        Pager(
            PagingConfig(
                pageSize = 30
            )
        ) {
            ProductPagingSource(
                queryProducts,
                sortOrder,
                productImageRepository,
                productInventoryRepository,
                reviewRepository
            )
        }

    // TODO("CRUD Operations for Product Collection")
    suspend fun getOne(productId: String): Product? {
        var foundProduct: Product? = null

        val productQuery = productCollectionRef.document(productId).get().await()
        productQuery?.let { doc ->
            val productImageList = productImageRepository.getAll(productId)

            val inventoryList = productInventoryRepository.getAll(productId)

            val reviewList = reviewRepository.getFive(productId)

            foundProduct = doc.toObject<Product>()!!.copy(
                productId = doc.id,
                productImageList = productImageList,
                inventoryList = inventoryList,
                reviewList = reviewList
            )
        }
        return foundProduct
    }

    suspend fun create(product: Product): Product {
        val result = productCollectionRef.add(product).await()
        if (result != null)
            return product.copy(productId = result.id)
        return Product()
    }

    suspend fun update(product: Product): Boolean {
        val productQuery = productCollectionRef.document(product.productId).get().await()
        if (productQuery != null) {
            val productToUpdateMap = mapOf<String, Any>(
                "name" to product.name,
                "description" to product.description,
                "imageUrl" to product.imageUrl,
                "price" to product.price
            )

            val result = productCollectionRef
                .document(product.productId)
                .set(productToUpdateMap, SetOptions.merge())
                .await()
            return result != null
        }
        return false
    }

    suspend fun submitReview(
        userInformation: UserInformation,
        rate: Double,
        desc: String,
        productId: String,
    ): Product? {
        var updatedProduct: Product? = getOne(productId)

        reviewRepository.insert(
            userInformation,
            rate,
            desc,
            productId
        )?.let {
            updatedProduct?.let {
                val updateProductMap = mapOf<String, Any>(
                    "totalRate" to it.totalRate + rate,
                    "numberOfReviews" to it.numberOfReviews + 1
                )

                productCollectionRef
                    .document(productId)
                    .update(updateProductMap)
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        updatedProduct = null
                        return@addOnFailureListener
                    }
            }
        }
        return updatedProduct
    }

    suspend fun delete(productId: String): Boolean {
        val result = productCollectionRef.document(productId).delete().await()
        return result != null
    }

    // SHIPPED
    suspend fun deductStockToCommittedCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() - orderDetail.quantity
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() + orderDetail.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
        return false
    }

    // CANCELED
    suspend fun deductCommittedToStockCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {

        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - orderDetail.quantity
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() + orderDetail.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
        return false
    }

    // COMPLETED
    suspend fun deductCommittedToSoldCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {

        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - orderDetail.quantity
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() + orderDetail.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "committed" to committedNewCount,
                            "sold" to soldNewCount
                        )

                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
        return false
    }

    // RETURNED
    suspend fun deductSoldToReturnedCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {

        val foundUser = accountRepository.get(userId)

        foundUser?.let {
            if (userId == it.userId && it.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() - orderDetail.quantity
                        val returnedNewCount =
                            inventoryQuery["returned"].toString().toLong() + orderDetail.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "sold" to soldNewCount,
                            "returned" to returnedNewCount
                        )

                        productCollectionRef.document(orderDetail.product.productId).collection(
                            INVENTORIES_SUB_COLLECTION
                        )
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
        return false
    }
}
