package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.WISH_LIST_SUB_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class WishItemRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val userWishListRef = db.collection(USERS_COLLECTION)

    suspend fun getAll(
        userId: String
    ): List<WishItem> {
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

        return wishList
    }

    suspend fun insert(
        userId: String,
        product: Product
    ): WishItem? {
        var createdWishItem: WishItem? = WishItem(
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

        createdWishItem?.let { w ->
            userWishListRef.document(userId)
                .collection(WISH_LIST_SUB_COLLECTION)
                .document(w.productId)
                .set(w)
                .addOnSuccessListener {
                }.addOnFailureListener {
                    createdWishItem = null
                    return@addOnFailureListener
                }
        }
        return createdWishItem
    }
}