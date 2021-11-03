package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_INFORMATION_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.Resource
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
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

}