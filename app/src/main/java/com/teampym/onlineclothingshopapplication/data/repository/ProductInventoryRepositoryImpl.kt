package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.util.INVENTORIES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ProductInventoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Inventory Collection operation - Add Stock, Create Inventory(size), Delete Inventory")

    suspend fun getAll(productId: String): List<Inventory> {
        val inventoriesQuery = productCollectionRef
            .document(productId)
            .collection(INVENTORIES_SUB_COLLECTION)
            .get()
            .await()

        val inventoryList = mutableListOf<Inventory>()

        if (inventoriesQuery != null) {
            for (document in inventoriesQuery.documents) {
                val copy =
                    document.toObject<Inventory>()!!.copy(id = document.id, productId = productId)
                inventoryList.add(copy)
            }
        }
        return inventoryList
    }

    suspend fun create(inventory: Inventory): Boolean {
        var isCreated = false
        productCollectionRef
            .document(inventory.productId)
            .collection(INVENTORIES_SUB_COLLECTION)
            .add(inventory)
            .addOnSuccessListener {
                isCreated = true
            }.addOnFailureListener {

            }
        return isCreated
    }

    suspend fun addStock(
        productId: String,
        inventoryId: String,
        stockToAdd: Long
    ): Boolean {
        val inventoryQuery = productCollectionRef
            .document(productId)
            .collection(INVENTORIES_SUB_COLLECTION)
            .document(inventoryId)
            .get()
            .await()

        if (inventoryQuery != null) {
            val updateStockMap = mapOf<String, Any>(
                "stock" to inventoryQuery["stock"].toString().toLong() + stockToAdd
            )

            var isUpdated = false
            productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(inventoryId)
                .set(updateStockMap, SetOptions.merge())
                .addOnSuccessListener {
                    isUpdated = true
                }.addOnFailureListener {

                }
            return isUpdated
        }
        return false
    }

    suspend fun delete(productId: String, inventoryId: String): Boolean {
        var isDeleted = false
        productCollectionRef
            .document(productId)
            .collection(INVENTORIES_SUB_COLLECTION)
            .document(inventoryId)
            .delete()
            .addOnSuccessListener {
                isDeleted = true
            }.addOnFailureListener {

            }
        return isDeleted
    }

}