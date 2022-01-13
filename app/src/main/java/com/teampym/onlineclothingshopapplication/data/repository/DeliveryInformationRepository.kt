package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_INFORMATION_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryInformationRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): List<DeliveryInformation> {
        return withContext(dispatcher) {
            val deliveryInformationList = mutableListOf<DeliveryInformation>()
            val deliveryInfoDocuments = userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .get()
                .await()

            if (deliveryInfoDocuments.documents.isNotEmpty()) {
                for (document in deliveryInfoDocuments.documents) {
                    val deliveryInformation = document
                        .toObject(DeliveryInformation::class.java)!!.copy(
                        id = document.id,
                        userId = userId
                    )

                    deliveryInformationList.add(deliveryInformation)
                }
            }
            deliveryInformationList
        }
    }

    @ExperimentalCoroutinesApi
    fun getFlow(userId: String): Flow<List<DeliveryInformation>> = callbackFlow {
        val deliveryInformationListener = userCollectionRef
            .document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    cancel(message = "Error fetching deliveryInformation", error)
                    return@addSnapshotListener
                }

                val deliveryInformationList = mutableListOf<DeliveryInformation>()

                for (document in value?.documents!!) {
                    val deliveryInformation = document
                        .toObject(DeliveryInformation::class.java)!!
                        .copy(
                            id = document.id,
                            userId = userId
                        )

                    deliveryInformationList.add(deliveryInformation)
                }
                offer(deliveryInformationList)
            }
        awaitClose {
            deliveryInformationListener.remove()
        }
    }

    suspend fun create(
        userId: String,
        deliveryInformation: DeliveryInformation
    ): DeliveryInformation? {
        return withContext(dispatcher) {
            val result = userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .add(deliveryInformation)
                .await()

            if (result != null) {
                return@withContext deliveryInformation.copy(id = result.id)
            }
            null
        }
    }

    suspend fun update(
        userId: String,
        deliveryInformation: DeliveryInformation
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            val deliveryInfoDocument = userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .get()
                .await()

            deliveryInfoDocument?.let { querySnapshot ->
                querySnapshot.forEach { doc ->
                    val deliveryInformationFromDb = doc
                        .toObject(DeliveryInformation::class.java)!!.copy(
                        id = doc.id,
                        userId = userId
                    )

                    if (deliveryInformation.id.isNotBlank() && deliveryInformation.id == deliveryInformationFromDb.id) {
                        doc.reference
                            .set(deliveryInformation, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isSuccessful = false
                                return@addOnFailureListener
                            }
                    }
                }
            }
            isSuccessful
        }
    }

    suspend fun changeDefault(
        userId: String,
        new: DeliveryInformation
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .whereEqualTo("isDefaultAddress", true)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.size == 1) {
                        isSuccessful =
                            switchDefaultAddressInFirestore(querySnapshot.documents[0], userId, new)
                    } else {
                        isSuccessful = false
                    }
                }.addOnFailureListener {
                    isSuccessful = false
                }
            isSuccessful
        }
    }

    private fun switchDefaultAddressInFirestore(
        doc: DocumentSnapshot,
        userId: String,
        new: DeliveryInformation
    ): Boolean {
        var isCompleted = true
        doc.let {
            it.reference.set(
                mutableMapOf<String, Any>(
                    "isDefaultAddress" to false
                ),
                SetOptions.merge()
            ).addOnSuccessListener {
                val updateNewInfoMap = mapOf<String, Any>(
                    "isDefaultAddress" to true
                )

                userCollectionRef
                    .document(userId)
                    .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                    .document(new.id)
                    .set(updateNewInfoMap, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }.addOnFailureListener {
                isCompleted = false
                return@addOnFailureListener
            }
        }
        return isCompleted
    }

    suspend fun delete(userId: String, deliveryInformation: DeliveryInformation): Boolean {
        return withContext(dispatcher) {
            try {
                userCollectionRef
                    .document(userId)
                    .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                    .document(deliveryInformation.id)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
