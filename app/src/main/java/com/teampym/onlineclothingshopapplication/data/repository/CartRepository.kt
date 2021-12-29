package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val userCartCollectionRef = db.collection(USERS_COLLECTION)
    private val productsCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Use a flow to get updates in cart collection instantly")
    fun getAll(userId: String): Flow<MutableList<Cart>> = callbackFlow {
        // Add snapshot listener to cart
        var cartListener: ListenerRegistration? = null
        if (userId.isNotBlank()) {
            cartListener = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        cancel(message = "Error fetching cart", error)
                        return@addSnapshotListener
                    }

                    val cartList = mutableListOf<Cart>()
                    if (value != null && value.documents.isNotEmpty()) {
                        // loop all items in cart
                        for (document in value.documents) {

                            var cartItem = document
                                .toObject(Cart::class.java)!!.copy(id = document.id)

                            val foundProduct = cartItem.product
                            foundProduct.roomId = cartItem.id
                            foundProduct.cartId = cartItem.id

                            val foundInventory = cartItem.inventory
                            foundInventory.pCartId = cartItem.id

                            cartItem.product = foundProduct
                            cartItem.inventory = foundInventory

                            // get corresponding product (cartItem.id == product.id)
                            CoroutineScope(Dispatchers.IO).launch {
                                cartItem = getUpdatedPriceAndStock(cartItem, userId)
                            }
                            cartList.add(cartItem)
                        }
                    }
                    offer(cartList)
                }
        }
        awaitClose {
            cartListener?.remove()
        }
    }

    private suspend fun getUpdatedPriceAndStock(
        cart: Cart,
        userId: String
    ): Cart {
        return withContext(Dispatchers.IO) {
            val mProduct = productsCollectionRef
                .document(cart.product.productId)
                .get()
                .await()

            if (mProduct != null) {
                val fetchedUpdatedProduct = mProduct.toObject<Product>()!!
                    .copy(
                        productId = mProduct.id,
                        roomId = cart.id,
                        cartId = cart.id
                    )

                // get corresponding inventory of a single product (product.id == inventory.productId)
                val mInventory = productsCollectionRef
                    .document(fetchedUpdatedProduct.productId)
                    .collection(INVENTORIES_SUB_COLLECTION)
                    .document(cart.inventory.inventoryId)
                    .get()
                    .await()

                if (mInventory != null) {
                    val fetchedUpdatedInventory = mInventory.toObject<Inventory>()!!
                        .copy(
                            inventoryId = mInventory.id,
                            pid = cart.product.productId,
                            pCartId = cart.id
                        )

                    cart.product = fetchedUpdatedProduct
                    cart.inventory = fetchedUpdatedInventory

                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(cart.id)
                        .set(cart, SetOptions.merge())
                        .await()
                }
            }
            cart
        }
    }

    suspend fun insert(
        userId: String,
        cartItem: Cart
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            val cartDocument = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartItem.inventory.inventoryId)
                .get()
                .await()

            // check if the productId is existing in the user's cart. if true then update, if false then just add it.
            if (cartDocument.data != null) {
                val cartItemFromDb = cartDocument
                    .toObject(Cart::class.java)!!.copy(id = cartDocument.id)

                if (cartItemFromDb.inventory.inventoryId == cartItem.inventory.inventoryId && cartItemFromDb.userId == userId) {

                    cartItemFromDb.quantity += 1
                    cartItemFromDb.subTotal = cartItemFromDb.calculatedTotalPrice.toDouble()

                    try {
                        userCartCollectionRef
                            .document(userId)
                            .collection(CART_SUB_COLLECTION)
                            .document(cartItem.id)
                            .set(cartItemFromDb, SetOptions.merge())
                            .await()
                    } catch (ex: Exception) {
                        isSuccessful = false
                    }
                }
            } else {
                cartItem.subTotal = cartItem.calculatedTotalPrice.toDouble()

                try {
                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(cartItem.id)
                        .set(cartItem, SetOptions.merge())
                        .await()
                } catch (ex: Exception) {
                    isSuccessful = false
                }
            }
            isSuccessful
        }
    }

    suspend fun update(
        userId: String,
        cart: List<Cart>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (item in cart) {
                item.subTotal = item.calculatedTotalPrice.toDouble()

                try {
                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(item.inventory.inventoryId)
                        .set(item, SetOptions.merge())
                        .await()
                } catch (ex: Exception) {
                    isSuccessful = false
                }
            }
            isSuccessful
        }
    }

    suspend fun delete(
        userId: String,
        cartId: String
    ): Boolean {
        return withContext(dispatcher) {

            try {
                userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(cartId)
                    .delete()
                    .await()

                return@withContext true
            } catch (ex: Exception) {
                return@withContext false
            }
        }
    }

    suspend fun deleteOutOfStockItems(
        userId: String,
        cartList: List<Cart>
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            for (item in cartList) {
                try {
                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(item.id)
                        .delete()
                        .await()
                } catch (ex: Exception) {
                    isSuccessful = false
                }
            }
            isSuccessful
        }
    }

    suspend fun deleteAll(
        userId: String
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true
            val cartDocuments = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .get()
                .await()

            if (cartDocuments.documents.isNotEmpty()) {
                for (item in cartDocuments.documents) {

                    try {
                        userCartCollectionRef
                            .document(userId)
                            .collection(CART_SUB_COLLECTION)
                            .document(item.id)
                            .delete()
                            .await()
                    } catch (ex: Exception) {
                        isSuccessful = false
                    }
                }
            }
            isSuccessful
        }
    }
}
