package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCT_IMAGES_SUB_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("ProductImages Collection operation - Add and Delete")
    suspend fun getAll(productId: String): List<ProductImage> {
        return withContext(dispatcher) {
            val productImageList = mutableListOf<ProductImage>()
            val productImageDocuments = productCollectionRef
                .document(productId)
                .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                .get()
                .await()

            if (productImageDocuments.documents.isNotEmpty()) {
                for (document in productImageDocuments.documents) {
                    val copy = document.toObject<ProductImage>()!!
                        .copy(id = document.id, productId = productId)
                    productImageList.add(copy)
                }
            }
            productImageList
        }
    }

    suspend fun create(productImage: ProductImage): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            val result = productCollectionRef
                .document(productImage.productId)
                .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                .add(productImage)
                .await()
            isCompleted = result != null
            isCompleted
        }
    }

    suspend fun delete(productImage: ProductImage): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            productCollectionRef
                .document(productImage.productId)
                .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                .document(productImage.id)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            isCompleted
        }
    }
}
