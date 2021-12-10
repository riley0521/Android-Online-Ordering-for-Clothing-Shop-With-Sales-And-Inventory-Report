package com.teampym.onlineclothingshopapplication.presentation.client.reviews

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Review
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ReviewPagingSource(
    private val userId: String?,
    private val queryReviews: Query
) : PagingSource<QuerySnapshot, Review>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Review>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Review> {
        return try {

            val currentPage = params.key ?: queryReviews
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryReviews.startAfter(lastVisibleItem).get().await()
            }

            val reviewList = mutableListOf<Review>()
            for (document in currentPage.documents) {
                val review = document.toObject<Review>()!!.copy(id = document.id)
                reviewList.add(review)
            }

            // Move user's review to the very top of the reviews.
            userId?.let { id ->
                val currentUser = reviewList.firstOrNull { it.userId == id }
                if(currentUser != null) {
                    reviewList.removeAt(reviewList.indexOf(currentUser))
                    reviewList.add(0, currentUser)
                }
            }

            LoadResult.Page(
                data = reviewList,
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
