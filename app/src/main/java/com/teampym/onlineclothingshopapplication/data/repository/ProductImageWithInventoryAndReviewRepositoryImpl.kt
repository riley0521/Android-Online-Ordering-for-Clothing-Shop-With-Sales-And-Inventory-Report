package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.*
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductImageWithInventoryAndReviewRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val accountRepository: AccountAndDeliveryInformationImpl
) {

    // READ Operation
    private val productCollectionRef = db.collection("Product")

    fun getProductsPagingSource(queryProducts: Query) =
        Pager(
            PagingConfig(
                pageSize = 30
            )
        ) {
            ProductPagingSource(queryProducts)
        }

    // TODO("CRUD Operations for Product Collection")
    // I don't know if this is necessary but just in case!
    suspend fun getOneProductWithImagesAndInventories(productId: String): Product? {
        val productQuery = productCollectionRef.document(productId).get().await()
        if (productQuery != null) {

            val product = productQuery.toObject(Product::class.java)
            if(product != null) {

                val productImagesQuery = productCollectionRef.document(productQuery.id).collection("productImages").get().await()
                val productImageList = mutableListOf<ProductImage>()
                productImagesQuery?.let { querySnapshot ->
                    for(document in querySnapshot.documents) {
                        val productImage = document.toObject(ProductImage::class.java)!!.copy(id = document.id)
                        productImageList.add(productImage)
                    }
                }

                val inventoriesQuery = productCollectionRef.document(productQuery.id).collection("inventories").get().await()
                val inventoryList = mutableListOf<Inventory>()
                inventoriesQuery?.let { querySnapshot ->
                    for(document in querySnapshot.documents) {
                        val inventory = document.toObject(Inventory::class.java)!!.copy(id = document.id)
                        inventoryList.add(inventory)
                    }
                }

                return product.copy(id = productQuery.id, productImages = productImageList, inventories = inventoryList)
            }
        }
        return null
    }

    suspend fun createProduct(product: Product): Product? {
        val result = productCollectionRef.add(product).await()
        if (result != null)
            return product.copy(id = result.id)
        return null
    }

    suspend fun updateProduct(product: Product): Boolean {
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

    suspend fun deleteProduct(productId: String): Boolean {
        val result = productCollectionRef.document(productId).delete().await()
        return result != null
    }

    // TODO("Inventory Collection operation - Add Stock, Create Inventory(size), Delete Inventory")
    suspend fun createInventory(inventory: Inventory): Inventory? {
        val result = productCollectionRef.document(inventory.productId).collection("inventories")
            .add(inventory).await()
        if (result != null)
            return inventory.copy(id = result.id)
        return null
    }

    suspend fun addStockToInventory(
        productId: String,
        inventoryId: String,
        stockToAdd: Long
    ): Boolean {
        val inventoryQuery =
            productCollectionRef.document(productId).collection("inventories").document(inventoryId)
                .get().await()
        if (inventoryQuery != null) {
            val updateStockMap = mapOf<String, Any>(
                "stock" to inventoryQuery["stock"].toString().toLong() + stockToAdd
            )

            val result = productCollectionRef.document(productId).collection("inventories")
                .document(inventoryId).set(updateStockMap, SetOptions.merge()).await()
            return result != null
        }
        return false
    }

    suspend fun deleteInventory(productId: String, inventoryId: String): Boolean {
        val result = productCollectionRef
            .document(productId)
            .collection("inventories")
            .document(inventoryId)
            .delete().await()
        return result != null
    }

    // TODO("ProductImages Collection operation - Add and Delete")
    suspend fun createProductImage(productImage: ProductImage): ProductImage? {
        val result =
            productCollectionRef.document(productImage.productId).collection("productImages")
                .add(productImage).await()
        if (result != null)
            return productImage.copy(id = result.id)
        return null
    }

    suspend fun deleteProductImage(productImage: ProductImage): Boolean {
        val result =
            productCollectionRef.document(productImage.productId).collection("productImages")
                .document(productImage.id).delete().await()
        return result != null
    }

    // TODO("I feel like there is something missing here.. I think I should use List<OrderDetail> instead of List<Cart>")
    // SHIPPED
    suspend fun deductStockToCommittedCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {
        val user = accountRepository.getUser(userId)

        user?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (item in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() - item.quantity
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() + item.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId)
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
    suspend fun deductCommittedToStockCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user = accountRepository.getUser(userId)

        user?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (item in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - item.quantity
                        val stockNewCount =
                            inventoryQuery["stock"].toString().toLong() + item.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "stock" to stockNewCount,
                            "committed" to committedNewCount
                        )

                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId)
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
    suspend fun deductCommittedToSoldCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user = accountRepository.getUser(userId)

        user?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (item in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val committedNewCount =
                            inventoryQuery["committed"].toString().toLong() - item.quantity
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() + item.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "committed" to committedNewCount,
                            "sold" to soldNewCount
                        )

                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId)
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
    suspend fun deductSoldToReturnedCount(userId: String, orderDetailList: List<OrderDetail>): Boolean {

        val user = accountRepository.getUser(userId)

        user?.let { userInfo ->
            if (userId == userInfo.userId && userInfo.userType == UserType.ADMIN.toString()) {
                for (item in orderDetailList) {
                    val inventoryQuery =
                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId).get().await()
                    if (inventoryQuery != null) {
                        val soldNewCount =
                            inventoryQuery["sold"].toString().toLong() - item.quantity
                        val returnedNewCount =
                            inventoryQuery["returned"].toString().toLong() + item.quantity
                        val updateProductsInventoryMap = mapOf<String, Any>(
                            "sold" to soldNewCount,
                            "returned" to returnedNewCount
                        )

                        productCollectionRef.document(item.productId).collection("inventories")
                            .document(item.inventoryId)
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