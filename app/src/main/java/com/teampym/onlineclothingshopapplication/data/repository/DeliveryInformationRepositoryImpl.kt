package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_INFORMATION_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeliveryInformationRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val deliveryInformationDao: DeliveryInformationDao
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): List<DeliveryInformation> {
        val deliveryInformationQuery = userCollectionRef
            .document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .get()
            .await()

        val deliveryInformationList = mutableListOf<DeliveryInformation>()
        if (deliveryInformationQuery.documents.isNotEmpty()) {
            for (document in deliveryInformationQuery.documents) {
                val deliveryInformation = document
                    .toObject(DeliveryInformation::class.java)!!.copy(
                    id = document.id,
                    isPrimary = document["isPrimary"].toString().toBoolean()
                )

                deliveryInformationList.add(deliveryInformation)
                deliveryInformationDao.insert(deliveryInformation)
            }
            return deliveryInformationList
        }
        return emptyList()
    }

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
                value?.let { querySnapshot ->
                    if (querySnapshot.documents.isNotEmpty()) {
                        for (document in querySnapshot.documents) {
                            val deliveryInformation = document
                                .toObject(DeliveryInformation::class.java)!!
                                .copy(
                                    id = document.id,
                                    userId = userId,
                                    isPrimary = document["isPrimary"].toString().toBoolean()
                                )

                            deliveryInformationList.add(deliveryInformation)
                            CoroutineScope(Dispatchers.IO).launch {
                                deliveryInformationDao.insert(deliveryInformation)
                            }
                        }
                        offer(deliveryInformationList)
                    }
                }
            }
        awaitClose {
            deliveryInformationListener.remove()
        }
    }

    suspend fun upsert(
        userId: String,
        deliveryInformation: DeliveryInformation
    ): Boolean {
        val isDeliveryInformationExistingQuery = userCollectionRef
            .document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .get()
            .await()

        if (isDeliveryInformationExistingQuery.documents.isNotEmpty()) {
            for (document in isDeliveryInformationExistingQuery.documents) {
                val deliveryInformationFromDb = document
                    .toObject(DeliveryInformation::class.java)!!.copy(id = document.id)

                if (deliveryInformation.contactNo == deliveryInformationFromDb.contactNo &&
                    deliveryInformation.region == deliveryInformationFromDb.region &&
                    deliveryInformation.streetNumber == deliveryInformationFromDb.streetNumber
                ) {
                    return false
                } else {
                    val updateDeliveryInfoMap = mapOf<String, Any>(
                        "name" to deliveryInformation.name,
                        "contactNo" to deliveryInformation.contactNo,
                        "region" to deliveryInformation.region,
                        "province" to deliveryInformation.province,
                        "city" to deliveryInformation.city,
                        "streetNumber" to deliveryInformation.streetNumber,
                        "postalCode" to deliveryInformation.postalCode,
                        "isPrimary" to deliveryInformation.isPrimary
                    )

                    var isUpdated = false
                    userCollectionRef
                        .document(userId)
                        .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                        .document(deliveryInformation.id)
                        .update(updateDeliveryInfoMap)
                        .addOnSuccessListener {
                            isUpdated = true

                            CoroutineScope(Dispatchers.IO).launch {
                                deliveryInformationDao.insert(deliveryInformation)
                            }
                        }.addOnFailureListener {
                        }
                    return isUpdated
                }
            }
        } else {
            var isCreated = false
            userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .add(deliveryInformation)
                .addOnSuccessListener {
                    isCreated = true

                    if (deliveryInformation.isPrimary) {
                        CoroutineScope(Dispatchers.IO).launch {
                            changeDefault(userId, deliveryInformation)
                            deliveryInformationDao.insert(deliveryInformation)
                        }
                    }
                }.addOnFailureListener {
                }
            return isCreated
        }
        return false
    }

    suspend fun changeDefault(
        userId: String,
        new: DeliveryInformation
    ): Boolean {
        var isCompleted = false
        userCollectionRef.document(userId).collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .whereEqualTo("isPrimary", true)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                isCompleted = switchDefaultAddressInFirestore(querySnapshot, userId, new)
            }.addOnFailureListener {
            }

        return isCompleted
    }

    private fun switchDefaultAddressInFirestore(
        querySnapshot: QuerySnapshot,
        userId: String,
        new: DeliveryInformation
    ): Boolean {
        var isSuccess = false

        querySnapshot.forEach { doc ->
            doc.reference.update(
                mutableMapOf<String, Any>(
                    "isPrimary" to false
                )
            ).addOnSuccessListener {
                val updateNewInfoMap = mapOf<String, Any>(
                    "isPrimary" to true
                )

                userCollectionRef
                    .document(userId)
                    .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                    .document(new.id)
                    .update(updateNewInfoMap)
                    .addOnSuccessListener {
                        isSuccess = true
                    }.addOnFailureListener {
                    }
            }.addOnFailureListener {
            }
        }
        return isSuccess
    }

    suspend fun delete(userId: String, deliveryInformation: DeliveryInformation): Boolean {
        var isDeleted = false

        userCollectionRef.document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .document(deliveryInformation.id)
            .delete()
            .addOnSuccessListener {
                isDeleted = true
            }.addOnCanceledListener {
            }
        return isDeleted
    }
}
