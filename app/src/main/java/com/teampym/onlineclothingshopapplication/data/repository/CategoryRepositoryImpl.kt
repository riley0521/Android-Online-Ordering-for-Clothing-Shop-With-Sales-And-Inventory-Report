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
    suspend fun createCategory(category: Category): Category? {
        val result = categoriesCollectionRef.add(category).await()
        if(result != null)
            return category.copy(id = result.id)
        return null
    }

    suspend fun updateCategory(category: Category): Category? {
        val categoryQuery = categoriesCollectionRef.document(category.id).get().await()
        if(categoryQuery != null) {
            val categoryUpdateMap = mapOf<String, Any>(
                "name" to category.name,
                "imageUrl" to category.imageUrl
            )

            val result = categoriesCollectionRef
                .document(category.id)
                .set(categoryUpdateMap, SetOptions.merge())
                .await()
            if(result != null)
                return category
        }
        return null
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        val result = categoriesCollectionRef.document(categoryId).delete().await()
        return result != null
    }
}