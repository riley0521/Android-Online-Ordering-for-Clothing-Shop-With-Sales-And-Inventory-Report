package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val productImageRepository: ProductImageRepositoryImpl,
    private val productInventoryRepository: ProductInventoryRepositoryImpl,
    private val reviewRepository: ReviewRepositoryImpl,
    val accountRepository: AccountRepositoryImpl
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

    suspend fun create(product: Product?): Product? {
        val createdProduct = withContext(Dispatchers.IO) {
            var createdProductTemp = product
            createdProductTemp?.let { p ->
                productCollectionRef
                    .add(p)
                    .addOnSuccessListener {
                        p.productId = it.id
                    }.addOnFailureListener {
                        createdProductTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext createdProductTemp
        }
        return createdProduct
    }

    suspend fun update(product: Product): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            val productToUpdateMap = mapOf<String, Any>(
                "name" to product.name,
                "description" to product.description,
                "imageUrl" to product.imageUrl,
                "price" to product.price
            )

            productCollectionRef
                .document(product.productId)
                .set(productToUpdateMap, SetOptions.merge())
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
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
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            productCollectionRef
                .document(productId)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    // SHIPPED
    suspend fun deductStockToCommittedCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        var isSuccessful = true
        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryQuery != null) {
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() - orderDetail.quantity
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() + orderDetail.quantity

                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isSuccessful = false
                                return@addOnFailureListener
                            }
                    } else {
                        isSuccessful = false
                    }
                }
            }
        }
        return isSuccessful
    }

    // CANCELED
    suspend fun deductCommittedToStockCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        var isSuccessful = true
        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - orderDetail.quantity
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() + orderDetail.quantity

                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isSuccessful = false
                                return@addOnFailureListener
                            }
                    } else {
                        isSuccessful = false
                    }
                }
            }
        }
        return isSuccessful
    }

    // COMPLETED
    suspend fun deductCommittedToSoldCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        var isSuccessful = true
        val foundUser = accountRepository.get(userId)

        foundUser?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - orderDetail.quantity
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() + orderDetail.quantity

                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "committed" to committedNewCount,
                            "sold" to soldNewCount
                        )

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isSuccessful = false
                                return@addOnFailureListener
                            }
                    } else {
                        isSuccessful = false
                    }
                }
            }
        }
        return isSuccessful
    }

    // RETURNED
    suspend fun deductSoldToReturnedCount(
        userId: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        var isSuccessful = true

        val foundUser = accountRepository.get(userId)

        foundUser?.let {
            if (userId == it.userId && it.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryQuery != null) {
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() - orderDetail.quantity
                        val returnedNewCount =
                            inventoryQuery["returned"].toString().toLong() + orderDetail.quantity

                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "sold" to soldNewCount,
                            "returned" to returnedNewCount
                        )

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isSuccessful = false
                                return@addOnFailureListener
                            }
                    } else {
                        isSuccessful = false
                    }
                }
            }
        }
        return isSuccessful
    }
}
