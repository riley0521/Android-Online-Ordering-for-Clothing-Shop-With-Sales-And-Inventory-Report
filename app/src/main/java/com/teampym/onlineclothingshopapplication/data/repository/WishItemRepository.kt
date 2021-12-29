package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.WISH_LIST_SUB_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishItemRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userWishListRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(
        userId: String
    ): List<WishItem> {
        return withContext(dispatcher) {
            val wishList = mutableListOf<WishItem>()
            val wishListQuery = userWishListRef
                .document(userId)
                .collection(WISH_LIST_SUB_COLLECTION)
                .get()
                .await()

            wishListQuery?.let { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    val wishItem = doc.toObject<WishItem>()!!.copy(productId = doc.id)
                    wishList.add(wishItem)
                }
            }
            wishList
        }
    }

    suspend fun insert(
        userId: String,
        product: Product
    ): WishItem? {
        return withContext(dispatcher) {
            val createdWishItem = WishItem(
                categoryId = product.categoryId,
                name = product.name,
                description = product.description,
                fileName = product.fileName,
                imageUrl = product.imageUrl,
                price = product.price,
                productId = product.productId,
                userId = userId,
                cartId = "",
                type = product.type,
                dateAdded = product.dateAdded,
                dateModified = product.dateModified,
                totalRate = product.totalRate,
                numberOfReviews = product.numberOfReviews
            )

            try {
                userWishListRef.document(userId)
                    .collection(WISH_LIST_SUB_COLLECTION)
                    .document(createdWishItem.productId)
                    .set(createdWishItem, SetOptions.merge())
                    .await()

                return@withContext createdWishItem
            } catch (ex: Exception) {
                return@withContext null
            }
        }
    }

    suspend fun remove(userId: String, product: Product): Boolean {
        return withContext(dispatcher) {
            try {
                userWishListRef
                    .document(userId)
                    .collection(WISH_LIST_SUB_COLLECTION)
                    .document(product.productId)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }
}
