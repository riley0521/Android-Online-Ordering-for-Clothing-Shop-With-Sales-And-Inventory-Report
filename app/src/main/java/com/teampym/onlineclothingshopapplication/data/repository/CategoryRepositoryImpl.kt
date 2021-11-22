package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.CATEGORIES_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val categoriesCollectionRef = db.collection(CATEGORIES_COLLECTION)

    fun getCategories(): CollectionReference {
        return categoriesCollectionRef
    }

    // TODO("CRUD Operation for Categories collection")
    suspend fun createCategory(category: Category?): Category? {
        var createdCategory = category

        category?.let { c ->
            categoriesCollectionRef
                .add(c)
                .addOnSuccessListener {
                    createdCategory?.id = it.id
                }.addOnFailureListener {
                    createdCategory = null
                    return@addOnFailureListener
                }
        }
        return createdCategory
    }

    suspend fun updateCategory(category: Category?): Category? {
        var updatedCategory = category

        val categoryQuery = category?.id?.let { categoriesCollectionRef.document(it).get().await() }
        if (categoryQuery != null) {
            val categoryUpdateMap = mapOf<String, Any>(
                "name" to category.name,
                "imageUrl" to category.imageUrl
            )

            categoriesCollectionRef
                .document(category.id)
                .set(categoryUpdateMap, SetOptions.merge())
                .addOnSuccessListener {
                }.addOnFailureListener {
                    updatedCategory = null
                    return@addOnFailureListener
                }
        }
        return updatedCategory
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        var isSuccessful = true
        categoriesCollectionRef
            .document(categoryId)
            .delete()
            .addOnSuccessListener {
            }.addOnFailureListener {
                isSuccessful = false
                return@addOnFailureListener
            }
        return isSuccessful
    }
}
