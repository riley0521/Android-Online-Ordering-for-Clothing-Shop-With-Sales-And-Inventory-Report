package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.util.AuditType
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
    private val auditTrailRepository: AuditTrailRepository,
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

    suspend fun create(username: String, category: Category): Category? {
        return withContext(dispatcher) {
            category.dateAdded = Utils.getTimeInMillisUTC()
            val result = categoriesCollectionRef
                .add(category)
                .await()

            if (result != null) {
                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username CREATED a category - ${category.name}",
                        type = AuditType.CATEGORY.name
                    )
                )

                return@withContext category.copy(id = result.id)
            } else {
                return@withContext null
            }
        }
    }

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

    suspend fun update(username: String, category: Category): Boolean {
        return withContext(dispatcher) {
            category.dateModified = Utils.getTimeInMillisUTC()

            try {
                categoriesCollectionRef
                    .document(category.id)
                    .set(category, SetOptions.merge())
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username UPDATED category - ${category.name}",
                        type = AuditType.CATEGORY.name
                    )
                )

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    // Delete All Products in selected category
    suspend fun delete(username: String, category: Category): Boolean {
        return withContext(dispatcher) {

            try {
                categoriesCollectionRef
                    .document(category.id)
                    .delete()
                    .await()

                auditTrailRepository.insert(
                    AuditTrail(
                        username = username,
                        description = "$username DELETED category - ${category.name}",
                        type = AuditType.CATEGORY.name
                    )
                )

                deleteImage(category.fileName)

                productRepository.deleteAllProductAndSubCollection(username, category.id)

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun deleteImage(fileName: String): Boolean {
        return withContext(dispatcher) {
            try {
                imageRef.child(CATEGORY_PATH + fileName)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
