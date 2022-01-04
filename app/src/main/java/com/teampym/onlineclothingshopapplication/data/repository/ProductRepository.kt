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
import com.teampym.onlineclothingshopapplication.data.models.*
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.util.*
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
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
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)
    private val imageRef = Firebase.storage.reference

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

                foundProduct.productImageList = productImageList.await()
                foundProduct.inventoryList = inventoryList.await()
                foundProduct.reviewList = reviewList.await()
            }
            foundProduct
        }
    }

    suspend fun create(product: Product): Product? {
        return withContext(dispatcher) {
            try {
                val result = productCollectionRef
                    .add(product)
                    .await()

                return@withContext product.copy(productId = result.id)
            } catch (ex: JavaLangException) {
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

    suspend fun update(product: Product): Boolean {
        return withContext(dispatcher) {
            product.dateModified = Utils.getTimeInMillisUTC()
            try {
                productCollectionRef
                    .document(product.productId)
                    .set(product, SetOptions.merge())
                    .await()

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
    ): Product? {
        return withContext(dispatcher) {
            var updatedProduct: Product? = getOne(item.product.productId)

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
                        val updated = orderDetailRepository.update(item)
                        if (!updated) {
                            updatedProduct = null
                        }
                    } catch (ex: JavaLangException) {
                        updatedProduct = null
                    }
                }
            }
            updatedProduct
        }
    }

    suspend fun delete(productId: String): Boolean {
        return withContext(dispatcher) {
            try {
                productCollectionRef
                    .document(productId)
                    .delete()
                    .await()

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
        userType: String,
        orderDetailList: List<OrderDetail>
    ): List<OrderDetail> {
        return withContext(dispatcher) {

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

                        try {
                            productCollectionRef
                                .document(orderDetail.product.productId)
                                .collection(INVENTORIES_SUB_COLLECTION)
                                .document(orderDetail.inventoryId)
                                .set(inventory, SetOptions.merge())
                                .await()
                        } catch (ex: JavaLangException) {
                            return@withContext emptyList()
                        }
                    }
                }
                return@withContext orderDetailList
            }
            return@withContext emptyList()
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
