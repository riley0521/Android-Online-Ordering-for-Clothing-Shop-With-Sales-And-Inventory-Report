package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.util.AUDIT_TRAILS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuditTrailRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val auditCollectionRef = db.collection(AUDIT_TRAILS_COLLECTION)

    // TODO("Convert this into a paging source.")
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
        val createdAudit = withContext(Dispatchers.IO) {
            var createdAuditTemp: AuditTrail? = audit
            audit?.let { a ->
                auditCollectionRef
                    .add(a)
                    .addOnSuccessListener {
                        createdAuditTemp?.id = it.id
                    }.addOnFailureListener {
                        createdAuditTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext createdAuditTemp
        }
        return createdAudit
    }
}
