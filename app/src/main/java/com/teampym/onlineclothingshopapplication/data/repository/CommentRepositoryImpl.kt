package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Comment
import com.teampym.onlineclothingshopapplication.data.util.COMMENTS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    suspend fun getAll(postId: String): List<Comment> {
        val commentList = mutableListOf<Comment>()

        val fetchCommentsQuery = postCollectionRef
            .document(postId)
            .collection(COMMENTS_SUB_COLLECTION)
            .get()
            .await()

        fetchCommentsQuery?.let { querySnapshot ->
            for (doc in querySnapshot.documents) {
                val comment = doc.toObject<Comment>()!!.copy(id = doc.id)
                commentList.add(comment)
            }
        }

        return commentList
    }

    suspend fun insert(postId: String, comment: Comment?): Comment? {
        var createdComment = comment

        comment?.let { c ->
            postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .add(c)
                .addOnSuccessListener {
                    createdComment?.id = it.id
                }.addOnFailureListener {
                    createdComment = null
                    return@addOnFailureListener
                }
        }

        return createdComment
    }

    suspend fun update(postId: String, comment: Comment?): Comment? {
        var updatedComment = comment

        comment?.let {
            val updateCommentMap = mapOf<String, Any>(
                "description" to it.description,
                "dateCommented" to it.dateCommented
            )

            postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .document(it.id)
                .set(updateCommentMap)
                .addOnSuccessListener {
                }.addOnFailureListener {
                    updatedComment = null
                    return@addOnFailureListener
                }
        }

        return updatedComment
    }

    suspend fun delete(postId: String, commentId: String): Boolean {
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

        return isSuccessful
    }
}
