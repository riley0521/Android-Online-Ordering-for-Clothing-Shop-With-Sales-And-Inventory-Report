package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCT_IMAGES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PRODUCT_PATH
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImageRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val productCollectionRef = db.collection(PRODUCTS_COLLECTION)
    private val imageRef = Firebase.storage.reference

    suspend fun getAll(productId: String): List<ProductImage> {
        return withContext(dispatcher) {
            val productImageList = mutableListOf<ProductImage>()
            val productImageDocs = productCollectionRef
                .document(productId)
                .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                .get()
                .await()

            if (productImageDocs.documents.isNotEmpty()) {
                for (document in productImageDocs.documents) {
                    val productImageObj = document.toObject<ProductImage>()!!
                        .copy(id = document.id, productId = productId)
                    productImageList.add(productImageObj)
                }
            }
            productImageList
        }
    }

    suspend fun insertAll(productImageList: List<ProductImage>): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (item in productImageList) {
                val result = productCollectionRef
                    .document(item.productId)
                    .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                    .add(item)
                    .await()
                isSuccessful = result != null
            }
            isSuccessful
        }
    }

    suspend fun updateAll(productImageList: List<ProductImage>): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (item in productImageList) {
                try {
                    productCollectionRef
                        .document(item.productId)
                        .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                        .document(item.id)
                        .set(item, SetOptions.merge())
                        .await()
                } catch (ex: Exception) {
                    isSuccessful = false
                }
            }
            isSuccessful
        }
    }

    suspend fun uploadImages(imgUriList: List<Uri>): List<ProductImage> {
        return withContext(dispatcher) {
            val productImageList = mutableListOf<ProductImage>()

            for (img in imgUriList) {
                val fileName = UUID.randomUUID().toString()
                val uploadImage = imageRef.child(PRODUCT_PATH + fileName)
                    .putFile(img)
                    .await()

                val url = uploadImage.storage.downloadUrl.await().toString()
                productImageList.add(
                    ProductImage(
                        productId = "",
                        fileName = fileName,
                        imageUrl = url
                    )
                )
            }

            productImageList
        }
    }

    suspend fun deleteAll(listOfImages: List<ProductImage>): Boolean {
        return withContext(dispatcher) {
            var isSuccess = true
            for (item in listOfImages) {
                isSuccess = delete(item)
            }
            isSuccess
        }
    }

    suspend fun delete(productImage: ProductImage): Boolean {
        return withContext(dispatcher) {
            try {
                productCollectionRef
                    .document(productImage.productId)
                    .collection(PRODUCT_IMAGES_SUB_COLLECTION)
                    .document(productImage.id)
                    .delete()
                    .await()

                imageRef.child(PRODUCT_PATH + productImage.fileName)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
