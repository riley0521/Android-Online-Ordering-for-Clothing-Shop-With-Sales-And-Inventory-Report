package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.util.AUDIT_TRAILS_COLLECTION
import com.teampym.onlineclothingshopapplication.presentation.admin.audit_history.HistoryLogPagingSource
import kotlinx.coroutines.CoroutineDispatcher
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

    fun getSome(queryHistoryLogs: Query) = Pager(
        PagingConfig(
            pageSize = 30,
            prefetchDistance = 30,
            enablePlaceholders = false
        )
    ) {
        HistoryLogPagingSource(
            queryHistoryLogs
        )
    }

    suspend fun insert(audit: AuditTrail): AuditTrail? {
        return withContext(dispatcher) {
            val result = auditCollectionRef
                .add(audit)
                .await()
            if (result != null) {
                return@withContext audit.copy(id = result.id)
            } else {
                return@withContext null
            }
        }
    }
}
