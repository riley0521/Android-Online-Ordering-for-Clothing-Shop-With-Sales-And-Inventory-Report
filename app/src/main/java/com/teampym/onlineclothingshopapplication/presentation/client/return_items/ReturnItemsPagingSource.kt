package com.teampym.onlineclothingshopapplication.presentation.client.return_items

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ReturnItemsPagingSource(
    private val queryOrderItems: Query
) : PagingSource<QuerySnapshot, OrderDetail>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, OrderDetail>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, OrderDetail> {
        return try {

            val currentPage = params.key ?: queryOrderItems
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryOrderItems.startAfter(lastVisibleItem).get().await()
            }

            val orderItemList = mutableListOf<OrderDetail>()
            for (document in currentPage.documents) {
                val orderItem = document.toObject<OrderDetail>()!!.copy(id = document.id)
                orderItemList.add(orderItem)
            }

            LoadResult.Page(
                data = orderItemList,
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
