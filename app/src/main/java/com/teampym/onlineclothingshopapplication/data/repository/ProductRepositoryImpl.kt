package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.*
import com.teampym.onlineclothingshopapplication.data.util.*
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val productImageRepository: ProductImageRepositoryImpl,
    private val productInventoryRepository: ProductInventoryRepositoryImpl,
    private val accountRepository: AccountRepositoryImpl
) {

    // READ Operation
    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    fun getSome(queryProducts: Query) =
        Pager(
            PagingConfig(
                pageSize = 30
            )
        ) {
            ProductPagingSource(queryProducts, productImageRepository, productInventoryRepository)
        }

    // TODO("CRUD Operations for Product Collection")
    suspend fun getOne(productId: String): Product? {
        val productQuery = productCollectionRef.document(productId).get().await()
        if (productQuery.data != null) {

            val product = productQuery.toObject(Product::class.java)!!.copy(id = productQuery.id)

            val productImageList = productImageRepository.getAll(productId)

            val inventoryList = productInventoryRepository.getAll(productId)

            return product.copy(id = productQuery.id, productImageList = productImageList, inventoryList = inventoryList)
        }
        return null
    }

    suspend fun create(product: Product): Product? {
        val result = productCollectionRef.add(product).await()
        if (result != null)
            return product.copy(id = result.id)
        return null
    }

    suspend fun update(product: Product): Boolean {
        val productQuery = productCollectionRef.document(product.id).get().await()
        if (productQuery != null) {
            val productToUpdateMap = mapOf<String, Any>(
                "name" to product.name,
                "description" to product.description,
                "imageUrl" to product.imageUrl,
                "price" to product.price,
                "flag" to product.flag
            )

            val result = productCollectionRef.document(product.id)
                .set(productToUpdateMap, SetOptions.merge()).await()
            return result != null
        }
        return false
    }

    suspend fun delete(productId: String): Boolean {
        val result = productCollectionRef.document(productId).delete().await()
        return result != null
    }

    // TODO("I feel like there is something missing here.. I think I should use List<OrderDetail> instead of List<Cart>")
    // SHIPPED
    suspend fun deductStockToCommittedCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {
        val user: UserInformation = when(val res = accountRepository.get(userId)) {
            is Resource.Error -> UserInformation()
            is Resource.Success -> res.res as UserInformation
        }

        user.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
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

                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
    }

    // CANCELED
    suspend fun deductCommittedToStockCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user: UserInformation = when(val res = accountRepository.get(userId)) {
            is Resource.Error -> UserInformation()
            is Resource.Success -> res.res as UserInformation
        }

        user.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
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

                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
    }

    // COMPLETED
    suspend fun deductCommittedToSoldCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user: UserInformation = when(val res = accountRepository.get(userId)) {
            is Resource.Error -> UserInformation()
            is Resource.Success -> res.res as UserInformation
        }

        user.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
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

                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
    }

    // RETURNED
    suspend fun deductSoldToReturnedCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user: UserInformation = when(val res = accountRepository.get(userId)) {
            is Resource.Error -> UserInformation()
            is Resource.Success -> res.res as UserInformation
        }

        user.let {
            if (userId == it.userId && it.userType == UserType.ADMIN.toString()) {
                for (orderDetail in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
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

                        productCollectionRef.document(orderDetail.product.id).collection(
                            INVENTORIES_SUB_COLLECTION)
                            .document(orderDetail.inventoryId)
                            .set(updateProductsInventoryMap, SetOptions.merge()).await()
                    }
                }
                return true
            }
            return false
        }
    }

}