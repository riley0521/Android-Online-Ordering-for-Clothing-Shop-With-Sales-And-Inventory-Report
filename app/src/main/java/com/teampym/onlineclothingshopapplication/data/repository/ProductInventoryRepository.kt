package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.util.AuditType
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
    private val auditTrailRepository: AuditTrailRepository,
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
                    val copy = document.toObject<Inventory>()!!.copy(
                        inventoryId = document.id,
                        pid = productId
                    )
                    inventoryList.add(copy)
                }
            }
            inventoryList
        }
    }

    suspend fun create(
        username: String,
        productName: String,
        inventory: Inventory
    ): Inventory? {
        return withContext(dispatcher) {
            val result = productCollectionRef
                .document(inventory.pid)
                .collection(INVENTORIES_SUB_COLLECTION)
                .add(inventory)
                .await()

            if (result != null) {
                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username CREATED inventory with size of ${inventory.size} for $productName",
                        type = AuditType.INVENTORY.name
                    )
                )

                return@withContext inventory.copy(inventoryId = result.id, productName = productName)
            } else {
                return@withContext null
            }
        }
    }

    suspend fun addStock(
        username: String,
        productName: String,
        productId: String,
        inventoryId: String,
        stockToAdd: Long
    ): Inventory? {
        return withContext(dispatcher) {
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

                try {
                    productCollectionRef
                        .document(productId)
                        .collection(INVENTORIES_SUB_COLLECTION)
                        .document(inventoryId)
                        .set(inventory, SetOptions.merge())
                        .await()

                    auditTrailRepository.insert(
                        AuditTrail(
                            username = username,
                            description = "$username ADDED $stockToAdd STOCKS of ${inventory.size} for $productName",
                            type = AuditType.INVENTORY.name
                        )
                    )

                    inventory.productName = productName

                    return@withContext inventory
                } catch (ex: Exception) {
                    return@withContext null
                }
            }
            null
        }
    }

    suspend fun deleteAll(username: String, listOfInventories: List<Inventory>, productName: String): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (item in listOfInventories) {
                isSuccessful = delete(username, item.pid, item.inventoryId, item.size, productName)
            }
            isSuccessful
        }
    }

    suspend fun delete(
        username: String,
        productId: String,
        inventoryId: String,
        size: String,
        productName: String
    ): Boolean {
        return withContext(dispatcher) {
            try {
                productCollectionRef
                    .document(productId)
                    .collection(INVENTORIES_SUB_COLLECTION)
                    .document(inventoryId)
                    .delete()
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username DELETED $size SIZE for $productName",
                        type = AuditType.INVENTORY.name
                    )
                )

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
