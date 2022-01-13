package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton
import java.lang.Exception as JavaLangException

@Singleton
class ProductRepository @Inject constructor(
    db: FirebaseFirestore,
    private val productImageRepository: ProductImageRepository,
    private val productInventoryRepository: ProductInventoryRepository,
    private val reviewRepository: ReviewRepository,
    private val orderDetailRepository: OrderDetailRepository,
    private val auditTrailRepository: AuditTrailRepository,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)
    private val imageRef = Firebase.storage.reference

    suspend fun deleteAllProductAndSubCollection(username: String, categoryId: String) {
        withContext(dispatcher) {
            val productDocs = productCollectionRef
                .whereEqualTo("categoryId", categoryId)
                .get()
                .await()

            productDocs?.let {
                for (item in productDocs.documents) {
                    val productObj = item.toObject<Product>()!!.copy(productId = item.id)
                    delete(username, productObj)
                }
            }
        }
    }

    fun getSome(user: UserWithWishList?, queryProducts: Query, sortOrder: SortOrder) =
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

                foundProduct.productImageList =
                    productImageList.await() as MutableList<ProductImage>
                foundProduct.inventoryList = inventoryList.await()
                foundProduct.reviewList = reviewList.await()
            }
            foundProduct
        }
    }

    suspend fun create(username: String, product: Product): Product? {
        return withContext(dispatcher) {
            val result = productCollectionRef
                .add(product)
                .await()

            if (result != null) {

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username CREATED product - ${product.name}",
                        type = AuditType.PRODUCT.name
                    )
                )

                return@withContext product.copy(productId = result.id)
            } else {
                return@withContext null
            }
        }
    }

    suspend fun uploadImage(imgProduct: Uri): UploadedImage {
        return withContext(dispatcher) {
            val fileName = UUID.randomUUID().toString()
            val uploadImage = imageRef.child(PRODUCT_PATH + fileName)
                .putFile(imgProduct)
                .await()

            val url = uploadImage.storage.downloadUrl.await().toString()
            UploadedImage(url, fileName)
        }
    }

    suspend fun update(username: String, product: Product): Boolean {
        return withContext(dispatcher) {
            product.dateModified = Utils.getTimeInMillisUTC()
            try {
                productCollectionRef
                    .document(product.productId)
                    .set(product, SetOptions.merge())
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED product - ${product.name}",
                        type = AuditType.PRODUCT.name
                    )
                )

                return@withContext true
            } catch (ex: JavaLangException) {
                return@withContext false
            }
        }
    }

    suspend fun submitReview(
        userInformation: UserInformation,
        rate: Double,
        desc: String,
        item: OrderDetail,
    ): OrderDetail? {
        return withContext(dispatcher) {
            val updatedProduct: Product? = getOne(item.product.productId)

            if (updatedProduct != null) {
                val addedReview = reviewRepository.insert(
                    userInformation,
                    rate,
                    desc,
                    item.product.productId,
                    item.size
                )

                if (addedReview != null) {

                    updatedProduct.totalRate += rate
                    updatedProduct.numberOfReviews++

                    try {
                        productCollectionRef
                            .document(updatedProduct.productId)
                            .set(updatedProduct, SetOptions.merge())
                            .await()

                        item.canAddReview = false
                        item.hasAddedReview = true
                        orderDetailRepository.update(item)

                        return@withContext item
                    } catch (ex: JavaLangException) {
                        return@withContext null
                    }
                }
            }
            return@withContext null
        }
    }

    suspend fun delete(username: String, product: Product): Boolean {
        return withContext(dispatcher) {
            try {

                val productImages = productImageRepository.getAll(product.productId)
                productImageRepository.deleteAll(productImages)

                val productInventories = productInventoryRepository.getAll(product.productId)
                productInventoryRepository.deleteAll(username, productInventories, product.name)

                productCollectionRef
                    .document(product.productId)
                    .delete()
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username DELETED product - ${product.name}",
                        type = AuditType.PRODUCT.name
                    )
                )

                deleteImage(product.fileName)

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun deleteImage(fileName: String): Boolean {
        return withContext(dispatcher) {
            try {
                imageRef.child(PRODUCT_PATH + fileName)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: JavaLangException) {
                return@withContext false
            }
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

                        try {
                            productCollectionRef
                                .document(orderDetail.product.productId)
                                .collection(INVENTORIES_SUB_COLLECTION)
                                .document(orderDetail.inventoryId)
                                .set(inventory, SetOptions.merge())
                                .await()
                        } catch (ex: JavaLangException) {
                            isSuccessful = false
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

                    try {
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(inventory, SetOptions.merge())
                            .await()
                    } catch (ex: JavaLangException) {
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
        orderDetailList: List<OrderDetail>
    ): Boolean {
        return withContext(dispatcher) {

            for (orderDetail in orderDetailList) {
                val inventoryDocument =
                    productCollectionRef
                        .document(orderDetail.product.productId)
                        .collection(INVENTORIES_SUB_COLLECTION)
                        .document(orderDetail.inventoryId)
                        .get()
                        .await()

                if (inventoryDocument != null) {
                    val inventory = inventoryDocument.toObject<Inventory>()!!.copy(
                        inventoryId = inventoryDocument.id
                    )
                    inventory.committed -= orderDetail.quantity
                    inventory.sold += orderDetail.quantity

                    try {
                        productCollectionRef
                            .document(orderDetail.product.productId)
                            .collection(INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(inventory, SetOptions.merge())
                            .await()

                        return@withContext true
                    } catch (ex: JavaLangException) {
                        return@withContext false
                    }
                }
            }
            return@withContext false
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

                        try {
                            productCollectionRef
                                .document(orderDetail.product.productId)
                                .collection(INVENTORIES_SUB_COLLECTION)
                                .document(orderDetail.inventoryId)
                                .set(inventory, SetOptions.merge())
                                .await()
                        } catch (ex: JavaLangException) {
                            isSuccessful = false
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
