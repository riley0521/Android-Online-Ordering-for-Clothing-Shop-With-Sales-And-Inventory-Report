package com.teampym.onlineclothingshopapplication.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.models.SizeChart
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.util.PRODUCT_PATH
import com.teampym.onlineclothingshopapplication.data.util.ROOT_PATH
import com.teampym.onlineclothingshopapplication.data.util.SIZE_CHART_DATA
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class SizeChartRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val sizeChartCollectionRef = db.collection(SIZE_CHART_DATA)
    private val imageRef = Firebase.storage.reference

    suspend fun getAll(): List<SizeChart> {
        return withContext(dispatcher) {
            val sizeChartList = mutableListOf<SizeChart>()

            val sizeDocs = sizeChartCollectionRef.get().await()

            sizeDocs?.let {
                for (item in sizeDocs.documents) {
                    val sizeChartObj = item.toObject<SizeChart>()!!.copy(id = item.id)
                    sizeChartList.add(sizeChartObj)
                }
            }

            return@withContext sizeChartList
        }
    }

    suspend fun create(sizeChart: SizeChart): SizeChart? {
        return withContext(dispatcher) {
            val result = sizeChartCollectionRef
                .add(sizeChart)
                .await()

            if (result != null) {
                return@withContext sizeChart.copy(id = result.id)
            } else {
                return@withContext null
            }
        }
    }

    suspend fun uploadImage(imgUri: Uri): UploadedImage {
        return withContext(dispatcher) {
            val fileName = UUID.randomUUID().toString()
            val uploadImage = imageRef.child(ROOT_PATH + fileName)
                .putFile(imgUri)
                .await()

            val url = uploadImage.storage.downloadUrl.await().toString()
            return@withContext UploadedImage(url, fileName)
        }
    }

    suspend fun delete(sizeChart: SizeChart): Boolean {
        return withContext(dispatcher) {
            try {
                sizeChartCollectionRef
                    .document(sizeChart.id)
                    .delete()
                    .await()

                imageRef.child(ROOT_PATH + sizeChart.fileName)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
