package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.FAQModel
import com.teampym.onlineclothingshopapplication.data.util.FAQ_DATA
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Singleton

@Singleton
class FAQRepository(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val faqCollectionRef = db.collection(FAQ_DATA)

    suspend fun getAll(): List<FAQModel> {
        return withContext(dispatcher) {
            val faqs = mutableListOf<FAQModel>()

            val faqDocs = faqCollectionRef
                .orderBy("question", Query.Direction.ASCENDING)
                .get()
                .await()

            faqDocs?.let {
                for (item in faqDocs.documents) {
                    val faqObj = item.toObject<FAQModel>()!!.copy(id = item.id)
                    faqs.add(faqObj)
                }
            }

            return@withContext faqs
        }
    }

    suspend fun create(faq: FAQModel): FAQModel? {
        return withContext(dispatcher) {
            val createdFaq = faqCollectionRef
                .add(faq)
                .await()

            if (createdFaq != null) {
                return@withContext faq.copy(id = createdFaq.id)
            } else {
                return@withContext null
            }
        }
    }

    suspend fun update(faq: FAQModel): Boolean {
        return withContext(dispatcher) {
            try {
                faqCollectionRef
                    .document(faq.id)
                    .set(faq, SetOptions.merge())
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun delete(id: String): Boolean {
        return withContext(dispatcher) {
            try {
                faqCollectionRef
                    .document(id)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
