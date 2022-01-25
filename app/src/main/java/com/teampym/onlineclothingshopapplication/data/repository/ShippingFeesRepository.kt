package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.ShippingFee
import com.teampym.onlineclothingshopapplication.data.util.SHIPPING_FEES_DATA
import com.teampym.onlineclothingshopapplication.data.util.SHIPPING_ID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShippingFeesRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val shippingFeesCollectionRef = db.collection(SHIPPING_FEES_DATA)

    suspend fun get(): ShippingFee? {
        return withContext(dispatcher) {
            val shippingFeeDoc = shippingFeesCollectionRef.document(SHIPPING_ID).get().await()

            if (shippingFeeDoc.data != null) {
                return@withContext shippingFeeDoc.toObject<ShippingFee>()
            }
            null
        }
    }

    suspend fun update(shippingFee: ShippingFee): Boolean {
        return withContext(dispatcher) {
            try {
                shippingFeesCollectionRef
                    .document(SHIPPING_ID)
                    .set(shippingFee)
                    .await()

                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
