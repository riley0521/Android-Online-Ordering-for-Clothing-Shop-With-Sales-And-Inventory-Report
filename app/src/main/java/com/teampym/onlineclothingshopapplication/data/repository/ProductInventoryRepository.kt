package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.util.INVENTORIES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    // TODO("Inventory Collection operation - Add Stock, Create Inventory(size), Delete Inventory")
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
                        document.toObject<Inventory>()!!.copy(inventoryId = document.id, pid = productId)
                    inventoryList.add(copy)
                }
            }
            inventoryList
        }
    }

    suspend fun create(inventory: Inventory): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            productCollectionRef
                .document(inventory.pid)
                .collection(INVENTORIES_SUB_COLLECTION)
                .add(inventory)
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            isCompleted
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
                val inventory = inventoryDocument.toObject<Inventory>()!!.copy(inventoryId = inventoryDocument.id)
                inventory.stock += stockToAdd
                productCollectionRef
                    .document(productId)
                    .collection(INVENTORIES_SUB_COLLECTION)
                    .document(inventoryId)
                    .set(inventory, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isSuccessful = false
                        return@addOnFailureListener
                    }
            }
            isSuccessful
        }
    }

    suspend fun delete(productId: String, inventoryId: String): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(inventoryId)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            isCompleted
        }
    }
}
