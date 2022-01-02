package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import com.teampym.onlineclothingshopapplication.presentation.client.news.NewsPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    db: FirebaseFirestore,
    private val likeRepository: LikeRepository,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    fun getSome(userId: String?, queryPost: Query) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            NewsPagingSource(
                userId,
                likeRepository,
                queryPost
            )
        }

    suspend fun insert(post: Post): Post? {
        return withContext(dispatcher) {
            try {
                post.dateCreated = System.currentTimeMillis()
                val result = postCollectionRef
                    .add(post)
                    .await()

                return@withContext post.copy(id = result.id)
            } catch (ex: Exception) {
                return@withContext null
            }
        }
    }

    suspend fun update(
        post: Post
    ): Boolean {
        return withContext(dispatcher) {

            val postDocument = postCollectionRef
                .document(post.id)
                .get()
                .await()

            if (postDocument != null) {
                val avatarUrl = post.avatarUrl ?: ""
                val imageUrl = post.imageUrl ?: ""

                val updatePostMap = mapOf<String, Any>(
                    "title" to post.title,
                    "description" to post.description,
                    "createdBy" to post.createdBy,
                    "avatarUrl" to avatarUrl,
                    "imageUrl" to imageUrl,
                    "dateCreated" to System.currentTimeMillis()
                )

                try {
                    postCollectionRef
                        .document(post.id)
                        .set(updatePostMap, SetOptions.merge())
                        .await()

                    return@withContext true
                } catch (ex: Exception) {
                    return@withContext false
                }
            }
            return@withContext false
        }
    }

    suspend fun updateLikeCount(postId: String, count: Long): Boolean {
        return withContext(dispatcher) {

            val postDocument = postCollectionRef
                .document(postId)
                .get()
                .await()

            postDocument?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                post.numberOfLikes = count

                try {
                    doc.reference
                        .set(post, SetOptions.merge())
                        .await()

                    return@withContext true
                } catch (ex: Exception) {
                    return@withContext false
                }
            }
            return@withContext false
        }
    }

    suspend fun updateCommentCount(postId: String, count: Long): Boolean {
        return withContext(dispatcher) {
            val postDocument = postCollectionRef
                .document(postId)
                .get()
                .await()

            postDocument?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                post.numberOfComments = count

                try {
                    doc.reference
                        .set(post, SetOptions.merge())
                        .await()

                    return@withContext true
                } catch (ex: Exception) {
                    return@withContext false
                }
            }
            return@withContext false
        }
    }
}
