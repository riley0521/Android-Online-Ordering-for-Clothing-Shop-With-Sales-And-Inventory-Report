package com.teampym.onlineclothingshopapplication.presentation.admin.accounts

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import kotlinx.coroutines.tasks.await
import java.io.IOException

class AccountPagingSource(
    private val queryAccounts: Query
): PagingSource<QuerySnapshot, UserInformation>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, UserInformation>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, UserInformation> {
        return try {

            val currentPage = params.key ?: queryAccounts
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryAccounts.startAfter(lastVisibleItem).get().await()
            }

            val accountList = mutableListOf<UserInformation>()
            for (document in currentPage.documents) {
                val user = document.toObject<UserInformation>()!!.copy(userId = document.id)
                accountList.add(user)
            }

            LoadResult.Page(
                data = accountList,
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