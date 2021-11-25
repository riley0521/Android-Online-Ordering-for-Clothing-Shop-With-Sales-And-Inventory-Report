package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.CATEGORIES_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val categoriesCollectionRef = db.collection(CATEGORIES_COLLECTION)

    suspend fun getCategories(): List<Category> {
        return withContext(dispatcher) {
            val categoryDocuments = categoriesCollectionRef
                .get()
                .await()

            val categoryList = mutableListOf<Category>()
            categoryDocuments.documents.let { docs ->
                for (doc in docs) {
                    val category = doc.toObject<Category>()!!.copy(id = doc.id)
                    categoryList.add(category)
                }
            }
            categoryList
        }
    }

    suspend fun createCategory(category: Category?): Category? {
        return withContext(dispatcher) {
            var createdCategory: Category? = category
            createdCategory?.let { c ->
                val result = categoriesCollectionRef
                    .add(c)
                    .await()
                if (result != null) {
                    createdCategory?.id = result.id
                } else {
                    createdCategory = null
                }
            }
            createdCategory
        }
    }

    suspend fun updateCategory(category: Category?): Category? {
        return withContext(dispatcher) {
            var updatedCategory: Category? = category
            updatedCategory?.let { c ->
                val categoryDocument = categoriesCollectionRef
                    .document(c.id)
                    .get()
                    .await()

                if (categoryDocument != null) {
                    categoriesCollectionRef
                        .document(c.id)
                        .set(c, SetOptions.merge())
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            updatedCategory = null
                            return@addOnFailureListener
                        }
                }
            }
            updatedCategory
        }
    }

    suspend fun deleteCategory(categoryId: String): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            categoriesCollectionRef
                .document(categoryId)
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
