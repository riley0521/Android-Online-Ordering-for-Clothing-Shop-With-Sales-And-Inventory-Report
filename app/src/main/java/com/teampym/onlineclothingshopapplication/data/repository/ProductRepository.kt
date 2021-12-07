package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    db: FirebaseFirestore,
    private val productImageRepository: ProductImageRepository,
    private val productInventoryRepository: ProductInventoryRepository,
    private val reviewRepository: ReviewRepository,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    fun getSome(user: UserInformation?, queryProducts: Query, sortOrder: SortOrder) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            ProductPagingSource(
                user,
                queryProducts,
                sortOrder,
                productImageRepository,
                productInventoryRepository,
                reviewRepository
            )
        }

    suspend fun getOne(productId: String): Product? {
        return withContext(dispatcher) {
            var foundProduct: Product? = null

            val productDocument = productCollectionRef.document(productId).get().await()
            if (productDocument != null) {
                val productImageList = async { productImageRepository.getAll(productId) }

                val inventoryList = async { productInventoryRepository.getAll(productId) }

                val reviewList = async { reviewRepository.getFive(productId) }

                foundProduct = productDocument.toObject<Product>()!!.copy(
                    productId = productDocument.id
                )

                foundProduct.productImageList = productImageList.await()
                foundProduct.inventoryList = inventoryList.await()
                foundProduct.reviewList = reviewList.await()
            }
            foundProduct
        }
    }

    suspend fun create(product: Product?): Product? {
        return withContext(dispatcher) {
            var createdProduct = product
            createdProduct?.let { p ->
                p.dateAdded = System.currentTimeMillis()
                productCollectionRef
                    .add(p)
                    .addOnSuccessListener {
                        createdProduct?.productId = it.id
                    }.addOnFailureListener {
                        createdProduct = null
                        return@addOnFailureListener
                    }
            }
            createdProduct
        }
    }

    suspend fun update(product: Product): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true

            product.dateModified = System.currentTimeMillis()
            val result = productCollectionRef
                .document(product.productId)
                .set(product, SetOptions.merge())
                .await()
            if (result == null) {
                isCompleted = false
            }
            isCompleted
        }
    }

    suspend fun submitReview(
        userInformation: UserInformation,
        rate: Double,
        desc: String,
        productId: String,
    ): Product? {
        return withContext(dispatcher) {
            var updatedProduct: Product? = getOne(productId)

            val addedReview = async {
                reviewRepository.insert(
                    userInformation,
                    rate,
                    desc,
                    productId
                )
            }

            addedReview.await()?.let {
                updatedProduct?.let { p ->
                    p.totalRate += rate
                    p.numberOfReviews + 1

                    productCollectionRef
                        .document(productId)
                        .set(p, SetOptions.merge())
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            updatedProduct = null
                            return@addOnFailureListener
                        }
                }
            }
            updatedProduct
        }
    }

    suspend fun delete(productId: String): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            productCollectionRef
                .document(productId)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            isCompleted
        }
    }

    // This will be called from category repository to delete all items in the specified
    // Category Id.
    suspend fun deleteAll(categoryId: String): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            val productDocuments = productCollectionRef
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()

            if (productDocuments.documents.isNotEmpty()) {
                for (doc in productDocuments.documents) {
                    val res = doc.reference.delete().await()
                    isSuccessful = res != null
                }
            }
            isSuccessful
        }
    }

    // SHIPPED
    suspend fun deductStockToCommittedCount(
        userType: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            if (userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryDocument =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryDocument != null) {
                        val inventory = inventoryDocument
                            .toObject<Inventory>()!!.copy(inventoryId = inventoryDocument.id)
                        inventory.stock -= orderDetail.quantity
                        inventory.committed += orderDetail.quantity

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(inventory, SetOptions.merge())
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
            isSuccessful
        }
    }

    // CANCELED
    // TODO("I think I should remove this because this is unnecessary.")
    suspend fun deductCommittedToStockCount(
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (orderDetail in orderDetailList) {
                val inventoryDocument =
                    productCollectionRef
                        .document(orderDetail.product.productId)
                        .collection(INVENTORIES_SUB_COLLECTION)
                        .document(orderDetail.inventoryId)
                        .get()
                        .await()

                if (inventoryDocument != null) {
                    val inventory = inventoryDocument
                        .toObject<Inventory>()!!.copy(inventoryId = inventoryDocument.id)
                    inventory.committed -= orderDetail.quantity
                    inventory.stock += orderDetail.quantity

                    val result = productCollectionRef
                        .document(orderDetail.product.productId)
                        .collection(INVENTORIES_SUB_COLLECTION)
                        .document(orderDetail.inventoryId)
                        .set(inventory, SetOptions.merge())
                        .await()
                    if (result == null) {
                        isSuccessful = false
                    }
                } else {
                    isSuccessful = false
                }
            }

            isSuccessful
        }
    }

    // COMPLETED
    suspend fun deductCommittedToSoldCount(
        userType: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            if (userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryDocument =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryDocument != null) {
                        val inventory = inventoryDocument
                            .toObject<Inventory>()!!.copy(inventoryId = inventoryDocument.id)
                        inventory.committed -= orderDetail.quantity
                        inventory.sold += orderDetail.quantity

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(inventory, SetOptions.merge())
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
            isSuccessful
        }
    }

    // RETURNED
    suspend fun deductSoldToReturnedCount(
        userType: String,
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            if (userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryDocument =
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .get()
                            .await()

                    if (inventoryDocument != null) {
                        val inventory = inventoryDocument
                            .toObject<Inventory>()!!.copy(inventoryId = inventoryDocument.id)
                        inventory.sold -= orderDetail.quantity
                        inventory.returned += orderDetail.quantity

                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(inventory, SetOptions.merge())
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
            isSuccessful
        }
    }
}
