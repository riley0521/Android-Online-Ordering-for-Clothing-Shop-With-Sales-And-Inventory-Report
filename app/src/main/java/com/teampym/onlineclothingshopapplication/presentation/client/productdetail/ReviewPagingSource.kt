package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.teampym.onlineclothingshopapplication.data.models.Review
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ReviewPagingSource(
    private val queryReviews: Query
): PagingSource<QuerySnapshot, Review>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Review>): QuerySnapshot? {
        TODO("Not yet implemented")
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
            for (review in currentPage.documents) {
                reviewList.add(
                    Review(
                        id = review.id,
                        productId = review.getString("productId")!!,
                        userId = review.getString("userId")!!,
                        userAvatar = review.getString("userAvatar")!!,
                        username = review.getString("username")!!,
                        rate = review.getDouble("rate")!!,
                        description = review.getString("description")!!,
                        dateReview = review.getString("dateReview")!!
                    )
                )
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