package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.WISH_LIST_SUB_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WishItemRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val wishItemDao: WishItemDao
) {

    private val userWishListRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(
        userId: String
    ): List<WishItem> {
        val wishListTemp = mutableListOf<WishItem>()
        val wishListQuery = userWishListRef
            .document(userId)
            .collection(WISH_LIST_SUB_COLLECTION)
            .get()
            .await()

        wishListQuery?.let { querySnapshot ->
            for (doc in querySnapshot.documents) {
                val wishItem = doc.toObject<WishItem>()!!.copy(productId = doc.id)
                wishListTemp.add(wishItem)
                wishItemDao.insert(wishItem)
            }
        }
        return wishListTemp
    }

    suspend fun insert(
        userId: String,
        product: Product
    ): WishItem? {
        val createdWishItem = withContext(Dispatchers.IO) {
            var createdWishItemTemp: WishItem? = WishItem(
                categoryId = product.categoryId,
                name = product.name,
                description = product.description,
                imageUrl = product.imageUrl,
                price = product.price,
                productId = product.productId,
                userId = userId,
                cartId = "",
                type = product.type,
            )

            createdWishItemTemp?.let { w ->
                userWishListRef.document(userId)
                    .collection(WISH_LIST_SUB_COLLECTION)
                    .document(w.productId)
                    .set(w)
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        createdWishItemTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext createdWishItemTemp
        }
        return createdWishItem
    }
}
