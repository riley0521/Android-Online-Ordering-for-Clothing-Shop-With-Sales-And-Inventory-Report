package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Return
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.util.IMAGES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.PROOF_PATH
import com.teampym.onlineclothingshopapplication.data.util.RETURNS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReturnRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val returnItemsCollectionRef = db.collection(RETURNS_COLLECTION)
    private val imageRef = Firebase.storage.reference

    suspend fun getByOrderItemId(orderItemId: String): Return? {
        return withContext(dispatcher) {

            val returnDoc = returnItemsCollectionRef
                .document(orderItemId)
                .get()
                .await()

            returnDoc?.let {
                val returnObj = returnDoc.toObject<Return>()!!

                val imageList = mutableListOf<UploadedImage>()
                val imageDocs = returnItemsCollectionRef
                    .document(orderItemId)
                    .collection(IMAGES_SUB_COLLECTION)
                    .get()
                    .await()

                imageDocs?.let {
                    for (item in imageDocs.documents) {
                        val imageObj = item.toObject<UploadedImage>()!!

                        imageList.add(imageObj)
                    }
                }

                return@withContext returnObj.copy(listOfImage = imageList)
            }
            null
        }
    }

    suspend fun create(returnItem: Return): Boolean {
        return withContext(dispatcher) {

            try {
                returnItemsCollectionRef
                    .document(returnItem.orderItemId)
                    .set(returnItem, SetOptions.merge())
                    .await()

                for (item in returnItem.listOfImage) {

                    returnItemsCollectionRef
                        .document(returnItem.orderItemId)
                        .collection(IMAGES_SUB_COLLECTION)
                        .add(item)
                        .await()
                }

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun delete(orderItemId: String): Boolean {
        return withContext(dispatcher) {
            val imageDocs = returnItemsCollectionRef
                .document(orderItemId)
                .collection(IMAGES_SUB_COLLECTION)
                .get()
                .await()

            imageDocs?.let {
                for (doc in imageDocs.documents) {
                    doc.reference.delete().await()
                }

                returnItemsCollectionRef
                    .document(orderItemId)
                    .delete()
                    .await()

                return@withContext true
            }
            return@withContext false
        }
    }

    suspend fun uploadImages(imgUriList: List<Uri>): List<UploadedImage> {
        return withContext(dispatcher) {
            val productImageList = mutableListOf<UploadedImage>()

            for (img in imgUriList) {
                val fileName = UUID.randomUUID().toString()
                val uploadImage = imageRef.child(PROOF_PATH + fileName)
                    .putFile(img)
                    .await()

                val url = uploadImage.storage.downloadUrl.await().toString()
                productImageList.add(
                    UploadedImage(
                        url,
                        fileName
                    )
                )
            }

            productImageList
        }
    }
}
