package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.util.INVENTORIES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductInventoryRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    suspend fun getAll(productId: String): List<Inventory> {
        return withContext(dispatcher) {
            val inventoryList = mutableListOf<Inventory>()
            val inventoryDocuments = productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .get()
                .await()

            if (inventoryDocuments.documents.isNotEmpty()) {
                for (document in inventoryDocuments.documents) {
                    val copy =
                        document.toObject<Inventory>()!!
                            .copy(inventoryId = document.id, pid = productId)
                    inventoryList.add(copy)
                }
            }
            inventoryList
        }
    }

    suspend fun create(inventory: Inventory): Inventory? {
        return withContext(dispatcher) {
            var createdInventory: Inventory? = inventory
            val result = productCollectionRef
                .document(inventory.pid)
                .collection(INVENTORIES_SUB_COLLECTION)
                .add(inventory)
                .await()
            if (result != null) {
                createdInventory?.inventoryId = result.id
            } else {
                createdInventory = null
            }
            createdInventory
        }
    }

    suspend fun addStock(
        productId: String,
        inventoryId: String,
        stockToAdd: Long
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            val inventoryDocument = productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(inventoryId)
                .get()
                .await()

            if (inventoryDocument != null) {
                val inventory = inventoryDocument.toObject<Inventory>()!!
                    .copy(inventoryId = inventoryDocument.id)
                inventory.stock += stockToAdd
                val result = productCollectionRef
                    .document(productId)
                    .collection(INVENTORIES_SUB_COLLECTION)
                    .document(inventoryId)
                    .set(inventory, SetOptions.merge())
                    .await()
                isSuccessful = result != null
            }
            isSuccessful
        }
    }

    suspend fun delete(productId: String, inventoryId: String): Boolean {
        return withContext(dispatcher) {
            val result = productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(inventoryId)
                .delete()
                .await()
            result != null
        }
    }
}
