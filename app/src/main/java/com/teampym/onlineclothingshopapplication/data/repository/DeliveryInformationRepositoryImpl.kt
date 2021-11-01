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
        val deliveryInformationQuery =
            userCollectionRef.document(userId).collection(DELIVERY_INFORMATION_SUB_COLLECTION).get()
                .await()
        val deliveryInformationList = mutableListOf<DeliveryInformation>()
        deliveryInformationQuery?.let { querySnapshot ->
            for (document in querySnapshot.documents) {
                val deliveryInformation =
                    document.toObject(DeliveryInformation::class.java)!!.copy(id = document.id)
                deliveryInformationList.add(deliveryInformation)
                deliveryInformationDao.insert(deliveryInformation)
            }
            return Resource.Success("Success", deliveryInformationList)
        }
        return Resource.Error("Failed", null)
    }

    suspend fun upsert(
        userId: String,
        deliveryInformation: DeliveryInformation
    ): Resource {
        val isDeliveryInformationExistingQuery = userCollectionRef.document(userId).collection(
            DELIVERY_INFORMATION_SUB_COLLECTION
        ).get().await()
        if (isDeliveryInformationExistingQuery != null) {
            for (document in isDeliveryInformationExistingQuery.documents) {
                val deliveryInfo =
                    document.toObject(DeliveryInformation::class.java)!!.copy(id = document.id)
                if (deliveryInformation.contactNo == deliveryInfo.contactNo &&
                    deliveryInformation.region == deliveryInfo.region &&
                    deliveryInformation.streetNumber == deliveryInfo.streetNumber
                ) {
                    val copy = deliveryInfo.copy(userId = "Existing")
                    return Resource.Success("Success", copy)
                }
            }
        } else {
            val result = userCollectionRef.document(userId).collection(
                DELIVERY_INFORMATION_SUB_COLLECTION
            ).add(deliveryInformation).await()
            if (result != null) {
                val res = deliveryInformation.copy(id = result.id)
                return Resource.Success("Success", res)
            }
        }
        return Resource.Error("Failed", null)
    }

}