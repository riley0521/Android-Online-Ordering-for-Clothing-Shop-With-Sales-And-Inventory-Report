package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Comment
import com.teampym.onlineclothingshopapplication.data.util.COMMENTS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    db: FirebaseFirestore,
    private val notificationTokenRepository: NotificationTokenRepository,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    // TODO("Create a paging source.")
    suspend fun getAll(postId: String): List<Comment> {
        return withContext(dispatcher) {
            val commentList = mutableListOf<Comment>()

            val commentDocuments = postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .get()
                .await()

            commentDocuments?.let { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    val comment = doc.toObject<Comment>()!!.copy(id = doc.id)
                    commentList.add(comment)
                }
            }
            commentList
        }
    }

    suspend fun insert(postId: String, comment: Comment): Comment? {
        return withContext(dispatcher) {
            val result = postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .add(comment)
                .await()

            if (result != null) {
                return@withContext comment.copy(id = result.id)
            }
            return@withContext null
        }
    }

    suspend fun update(postId: String, comment: Comment): Comment? {
        return withContext(dispatcher) {
            try {
                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .document(comment.id)
                    .set(comment, SetOptions.merge())
                    .await()

                return@withContext comment
            } catch (ex: Exception) {
                return@withContext null
            }
        }
    }

    suspend fun delete(postId: String, commentId: String): Boolean {
        return withContext(dispatcher) {
            try {
                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .document(commentId)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
