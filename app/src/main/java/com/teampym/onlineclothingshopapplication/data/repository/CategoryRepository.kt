package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.util.CATEGORIES_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.CATEGORY_PATH
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    db: FirebaseFirestore,
    private val productRepository: ProductRepository,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val categoriesCollectionRef = db.collection(CATEGORIES_COLLECTION)

    private val imageRef = Firebase.storage.reference

    suspend fun getAll(): List<Category> {
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

    suspend fun create(category: Category?): Category? {
        return withContext(dispatcher) {
            var createdCategory: Category? = category
            if (createdCategory != null) {
                val result = categoriesCollectionRef
                    .add(createdCategory)
                    .await()

                if (result != null) {
                    createdCategory.id = result.id
                } else {
                    createdCategory = null
                }
            }
            createdCategory
        }
    }

    // TODO(Upload image in public/product/ folder)
    suspend fun uploadImage(imgCategory: Uri): UploadedImage {
        return withContext(dispatcher) {
            val fileName = UUID.randomUUID().toString()
            val uploadImage = imageRef
                .child(CATEGORY_PATH + fileName)
                .putFile(imgCategory)
                .await()

            val url = uploadImage.storage.downloadUrl.await().toString()
            UploadedImage(url, fileName)
        }
    }

    suspend fun update(category: Category?): Category? {
        return withContext(dispatcher) {
            var updatedCategory: Category? = category
            if (updatedCategory != null) {
                updatedCategory.dateModified = Utils.getTimeInMillisUTC()

                val result = categoriesCollectionRef
                    .document(updatedCategory.id)
                    .set(updatedCategory, SetOptions.merge())
                    .await()

                if (result == null) {
                    updatedCategory = null
                }
            }
            updatedCategory
        }
    }

    // Delete All Products in selected category
    suspend fun delete(categoryId: String): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            val result = categoriesCollectionRef
                .document(categoryId)
                .delete()
                .await()

            if (result != null) {
                isSuccessful = productRepository.deleteAll(categoryId)
            }

            isSuccessful
        }
    }
}
