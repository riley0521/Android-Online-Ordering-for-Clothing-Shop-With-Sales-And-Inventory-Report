package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.util.INVENTORIES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProductInventoryRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
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

        if (inventoriesQuery.documents.isNotEmpty()) {
            for (document in inventoriesQuery.documents) {
                val copy =
                    document.toObject<Inventory>()!!.copy(inventoryId = document.id, pid = productId)
                inventoryList.add(copy)
            }
        }
        return inventoryList
    }

    suspend fun create(inventory: Inventory): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
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
            return@withContext isCompleted
        }
        return isSuccessful
    }

    suspend fun addStock(
        productId: String,
        inventoryId: String,
        stockToAdd: Long
    ): Boolean {
        var isSuccessful = true

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
            productCollectionRef
                .document(productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(inventoryId)
                .set(updateStockMap, SetOptions.merge())
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isSuccessful = false
                    return@addOnFailureListener
                }
            return isSuccessful
        }
        return false
    }

    suspend fun delete(productId: String, inventoryId: String): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
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
            return@withContext isCompleted
        }
        return isSuccessful
    }
}
