package com.teampym.onlineclothingshopapplication.presentation.client.news

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bumptech.glide.load.HttpException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.repository.LikeRepository
import kotlinx.coroutines.tasks.await
import java.io.IOException

class NewsPagingSource(
    private val userId: String?,
    private val likeRepository: LikeRepository,
    private val queryPosts: Query
) : PagingSource<QuerySnapshot, Post>() {

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return state.anchorPosition?.let { anchorPosition ->
            // This loads starting from previous page, but since PagingConfig.initialLoadSize spans
            // multiple pages, the initial load will still load items centered around
            // anchorPosition. This also prevents needing to immediately launch prepend due to
            // prefetchDistance.
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {

            val currentPage = params.key ?: queryPosts
                .get()
                .await()

            var nextPage: QuerySnapshot? = null

            if (currentPage.size() > 30) {
                val lastVisibleItem = currentPage!!.documents[currentPage.size() - 1]
                nextPage = queryPosts.startAfter(lastVisibleItem).get().await()
            }

            val postList = mutableListOf<Post>()
            for (document in currentPage.documents) {
                val post = document.toObject<Post>()!!.copy(id = document.id)
                postList.add(post)
            }

            userId?.let {
                postList.map { p ->
                    p.isLikedByCurrentUser = likeRepository.isLikedByCurrentUser(p.id, it)
                }
            }

            LoadResult.Page(
                data = postList,
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
