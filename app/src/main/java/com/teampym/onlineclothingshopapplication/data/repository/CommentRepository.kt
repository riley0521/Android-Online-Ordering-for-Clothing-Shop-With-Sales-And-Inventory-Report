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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    db: FirebaseFirestore,
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

    suspend fun insert(postId: String, comment: Comment?): Comment? {
        return withContext(dispatcher) {
            var createdComment = comment
            createdComment?.let { c ->
                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .add(c)
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        createdComment = null
                        return@addOnFailureListener
                    }
            }
            createdComment
        }
    }

    suspend fun update(postId: String, comment: Comment?): Comment? {
        return withContext(dispatcher) {
            var updatedComment = comment
            updatedComment?.let { c ->
                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .document(c.id)
                    .set(c, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        updatedComment = null
                        return@addOnFailureListener
                    }
            }
            updatedComment
        }
    }

    suspend fun delete(postId: String, commentId: String): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .document(commentId)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isSuccessful = false
                    return@addOnFailureListener
                }
            isSuccessful
        }
    }
}
