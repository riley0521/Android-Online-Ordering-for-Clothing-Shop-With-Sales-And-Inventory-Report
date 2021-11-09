package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_INFORMATION_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Resource
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeliveryInformationRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val deliveryInformationDao: DeliveryInformationDao
) {

    private val userCollectionRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(userId: String): Resource {
        val deliveryInformationQuery = userCollectionRef
            .document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .get()
            .await()

        val deliveryInformationList = mutableListOf<DeliveryInformation>()
        if (deliveryInformationQuery.documents.isNotEmpty()) {
            for (document in deliveryInformationQuery.documents) {
                val deliveryInformation = document
                    .toObject(DeliveryInformation::class.java)!!.copy(id = document.id)

                deliveryInformationList.add(deliveryInformation)
                deliveryInformationDao.insert(deliveryInformation)
            }
            return Resource.Success("Success", deliveryInformationList)
        }
        return Resource.Error("Failed", emptyList<DeliveryInformation>())
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
                if (value?.documents!!.isNotEmpty()) {
                    for (document in value.documents) {
                        val deliveryInformation = document
                            .toObject(DeliveryInformation::class.java)!!.copy(id = document.id)

                        deliveryInformationList.add(deliveryInformation)
                        CoroutineScope(Dispatchers.IO).launch {
                            deliveryInformationDao.insert(deliveryInformation)
                        }
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
    ): Resource {
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
                    return Resource.Success("Failed", false)
                } else {
                    val updateDeliveryInfoMap = mapOf<String, Any>(
                        "name" to deliveryInformation.name,
                        "contactNo" to deliveryInformation.contactNo,
                        "region" to deliveryInformation.region,
                        "province" to deliveryInformation.province,
                        "city" to deliveryInformation.city,
                        "streetNumber" to deliveryInformation.streetNumber,
                        "postalCode" to deliveryInformation.postalCode,
                        "default" to deliveryInformation.default
                    )

                    var isUpdated = false
                    userCollectionRef
                        .document(userId)
                        .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                        .document(deliveryInformation.id)
                        .update(updateDeliveryInfoMap)
                        .addOnSuccessListener {
                            isUpdated = true
                        }.addOnFailureListener {
                        }
                    return Resource.Success("Success", isUpdated)
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
                }.addOnFailureListener {
                }
            return Resource.Success("Success", isCreated)
        }
        return Resource.Error("Failed", false)
    }

    suspend fun changeDefault(
        userId: String,
        old: DeliveryInformation?,
        new: DeliveryInformation
    ): Boolean {
        var isCompleted = false

        val updateOldInfoMap = mapOf<String, Any>(
            "default" to false,
        )

        old?.id?.let {
            userCollectionRef
                .document(userId)
                .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
                .document(it)
                .update(updateOldInfoMap)
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                }
        }

        val updateNewInfoMap = mapOf<String, Any>(
            "default" to true
        )

        userCollectionRef
            .document(userId)
            .collection(DELIVERY_INFORMATION_SUB_COLLECTION)
            .document(new.id)
            .update(updateNewInfoMap)
            .addOnSuccessListener {
                isCompleted = true
            }.addOnFailureListener {
                isCompleted = false
            }

        return isCompleted
    }
}
