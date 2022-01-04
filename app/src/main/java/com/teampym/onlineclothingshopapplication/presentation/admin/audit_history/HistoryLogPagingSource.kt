package com.teampym.onlineclothingshopapplication.presentation.admin.audit_history

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import kotlinx.coroutines.tasks.await
import java.io.IOException

class HistoryLogPagingSource(
    private val queryHistoryLogs: Query
) : PagingSource<QuerySnapshot, AuditTrail>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, AuditTrail>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, AuditTrail> {
        return try {

            val currentPage = params.key ?: queryHistoryLogs
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryHistoryLogs.startAfter(lastVisibleItem).get().await()
            }

            val historyLogList = mutableListOf<AuditTrail>()
            for (document in currentPage.documents) {
                val historyLog = document.toObject<AuditTrail>()!!.copy(id = document.id)
                historyLogList.add(historyLog)
            }

            LoadResult.Page(
                data = historyLogList,
                prevKey = null,
                nextKey = nextPage
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
