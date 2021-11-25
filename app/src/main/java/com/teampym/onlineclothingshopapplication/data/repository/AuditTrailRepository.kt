package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.util.AUDIT_TRAILS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditTrailRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val auditCollectionRef = db.collection(AUDIT_TRAILS_COLLECTION)

    // TODO("Convert this into a paging source.")
    @ExperimentalCoroutinesApi
    suspend fun getAll(): Flow<List<AuditTrail>> = callbackFlow {
        val auditTrailListener = auditCollectionRef
            .addSnapshotListener { value, error ->
                if (error != null) {
                    cancel(message = "Error fetching audits", error)
                    return@addSnapshotListener
                }

                val auditTrailList = mutableListOf<AuditTrail>()
                value?.let { querySnapshot ->
                    for (doc in querySnapshot) {
                        val audit = doc.toObject<AuditTrail>().copy(id = doc.id)
                        auditTrailList.add(audit)
                    }
                    offer(auditTrailList)
                }
            }
        awaitClose {
            auditTrailListener.remove()
        }
    }

    suspend fun insert(audit: AuditTrail?): AuditTrail? {
        return withContext(dispatcher) {
            var createdAudit: AuditTrail? = audit
            audit?.let { a ->
                val result = auditCollectionRef
                    .add(a)
                    .await()
                if (result != null) {
                    createdAudit?.id = result.id
                } else {
                    createdAudit = null
                }
            }
            createdAudit
        }
    }
}
