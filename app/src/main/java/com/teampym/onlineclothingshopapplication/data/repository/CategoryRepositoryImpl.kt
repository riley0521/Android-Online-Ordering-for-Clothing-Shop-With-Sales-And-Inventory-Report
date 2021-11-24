package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.CATEGORIES_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val categoriesCollectionRef = db.collection(CATEGORIES_COLLECTION)

    suspend fun getCategories(): List<Category> {
        val categoryList = withContext(Dispatchers.IO) {
            val categoriesQuery = categoriesCollectionRef
                .get()
                .await()

            val categoryListTemp = mutableListOf<Category>()
            categoriesQuery.documents.let { docs ->
                for (doc in docs) {
                    val category = doc.toObject<Category>()!!.copy(id = doc.id)
                    categoryListTemp.add(category)
                }
            }
            return@withContext categoryListTemp
        }
        return categoryList
    }

    suspend fun createCategory(category: Category?): Category? {
        val createdCategory = withContext(Dispatchers.IO) {
            var createdCategoryTemp: Category? = category
            createdCategoryTemp?.let { c ->
                categoriesCollectionRef
                    .add(c)
                    .addOnSuccessListener {
                        c.id = it.id
                    }.addOnFailureListener {
                        createdCategoryTemp = null
                        return@addOnFailureListener
                    }
                return@withContext createdCategoryTemp
            }
            null
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
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            categoriesCollectionRef
                .document(categoryId)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
    }
}
