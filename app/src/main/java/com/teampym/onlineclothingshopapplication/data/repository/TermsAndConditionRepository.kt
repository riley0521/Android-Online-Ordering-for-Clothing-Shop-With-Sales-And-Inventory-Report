package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.TermsAndCondition
import com.teampym.onlineclothingshopapplication.data.util.TERMS_DATA
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TermsAndConditionRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val tcCollectionRef = db.collection(TERMS_DATA)

    suspend fun get(): TermsAndCondition? {
        return withContext(dispatcher) {
            val tcDoc = tcCollectionRef.get().await()

            if (tcDoc != null) {
                val firstObj = tcDoc.documents[0]

                return@withContext firstObj.toObject<TermsAndCondition>()!!.copy(id = firstObj.id)
            } else {
                return@withContext null
            }
        }
    }

    suspend fun update(termsAndCondition: TermsAndCondition): Boolean {
        return withContext(dispatcher) {
            try {
                tcCollectionRef.document(termsAndCondition.id).set(
                    termsAndCondition,
                    SetOptions.merge()
                ).await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
