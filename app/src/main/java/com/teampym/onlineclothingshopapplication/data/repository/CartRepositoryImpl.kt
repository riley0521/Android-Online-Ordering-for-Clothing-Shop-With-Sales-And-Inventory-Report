package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    val cartDao: CartDao
) {

    private val userCartCollectionRef = db.collection(USERS_COLLECTION)
    private val productsCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Use a flow to get updates in cart collection instantly")
    @ExperimentalCoroutinesApi
    fun getAll(userId: String): Flow<List<Cart>> = callbackFlow {
        // Add snapshot listener to cart
        val cartListener = userCartCollectionRef.document(userId)
            .collection(CART_SUB_COLLECTION)
            .addSnapshotListener { querySnapshot, firebaseException ->
                if (firebaseException != null) {
                    cancel(message = "Error fetching cart", firebaseException)
                    return@addSnapshotListener
                }

                val cartList = mutableListOf<Cart>()
                querySnapshot?.let { snapshot ->
                    // loop all items in cart
                    for (document in snapshot.documents) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val cartItem =
                                document.toObject(Cart::class.java)?.copy(id = document.id)
                            cartItem?.let { cart ->
                                val foundProduct =
                                    cart.product.copy(roomId = cart.id, cartId = cart.id)
                                val foundInventory = cart.inventory.copy(pCartId = cart.id)

                                cart.product = foundProduct
                                cart.inventory = foundInventory
                                // get corresponding product (cartItem.id == product.id)
                                val finalC = getUpdatedPriceAndStock(cart, userId, document.id)
                                cartList.add(finalC)
                                cartDao.insert(finalC)
                            }
                        }
                    }
                    offer(cartList)
                }
            }

        awaitClose() {
            cartListener.remove()
        }
    }

    private suspend fun getUpdatedPriceAndStock(
        cart: Cart,
        userId: String?,
        cartId: String
    ): Cart {
        val productQuery = productsCollectionRef
            .document(cartId)
            .get()
            .await()

        if (productQuery?.data != null) {

            val fetchedUpdatedProduct = productQuery.toObject<Product>()?.copy(
                productId = productQuery.id,
                roomId = cart.id,
                cartId = cart.id
            )

            // get corresponding inventory of a single product (product.id == inventory.productId)
            var productNotNull = Product()
            fetchedUpdatedProduct?.let {
                productNotNull = it
            }

            val inventoryQuery = productsCollectionRef
                .document(productNotNull.productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(cart.inventory.inventoryId)
                .get()
                .await()

            if (inventoryQuery.data != null) {
                val fetchedUpdatedInventory = inventoryQuery.toObject<Inventory>()?.copy(
                    inventoryId = inventoryQuery.id,
                    pid = cart.product.productId,
                    pCartId = cart.id
                )

                var inventoryNotNull = Inventory()
                fetchedUpdatedInventory?.let {
                    inventoryNotNull = it
                }

                // update finalCartItemTemp subTotal
                cart.subTotal = cart.calculatedTotalPrice.toDouble()
                // create update map.
                val updatePriceOfProductInCart = mapOf<String, Any>(
                    "product.price" to productNotNull.price,
                    "inventory.stock" to inventoryNotNull.stock,
                    "subTotal" to cart.subTotal,
                )

                userId?.let { id ->
                    val res = userCartCollectionRef
                        .document(id)
                        .collection(CART_SUB_COLLECTION)
                        .document(cartId)
                        .update(updatePriceOfProductInCart)
                        .await()
                    if (res != null) {
                        cart.product = productNotNull
                        cart.inventory = inventoryNotNull
                    }
                }
            }
        }
        return cart
    }

    suspend fun insert(
        userId: String,
        cartItem: Cart
    ): Boolean {

        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            val cartQuery = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartItem.inventory.inventoryId)
                .get()
                .await()

            // check if the productId is existing in the user's cart. if true then update, if false then just add it.
            if (cartQuery.data != null) {
                val cartItemFromDb =
                    cartQuery.toObject(Cart::class.java)?.copy(id = cartQuery.id)

                cartItemFromDb?.let { obj ->
                    if (obj.inventory.inventoryId == cartItem.inventory.inventoryId && obj.userId == userId) {

                        obj.quantity += 1
                        // every time you get calculatedTotalPrice in Cart data class.
                        // it will calculate quantity * product.price
                        obj.subTotal = obj.calculatedTotalPrice.toDouble()
                        val updateQuantityOfItemInCart = mapOf<String, Any>(
                            "quantity" to obj.quantity,
                            "subTotal" to obj.subTotal
                        )

                        userCartCollectionRef
                            .document(userId)
                            .collection(CART_SUB_COLLECTION)
                            .document(obj.id)
                            .set(updateQuantityOfItemInCart, SetOptions.merge())
                            .addOnSuccessListener {
                            }.addOnFailureListener {
                                isCompleted = false
                                return@addOnFailureListener
                            }
                        return@withContext isCompleted
                    }
                }
            } else {
                cartItem.subTotal = cartItem.calculatedTotalPrice.toDouble()

                userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(cartItem.inventory.inventoryId)
                    .set(cartItem)
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
                return@withContext isCompleted
            }
            false
        }
        return isSuccessful
    }

    suspend fun update(
        userId: String,
        cart: List<Cart>
    ): Boolean {

        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            for (item in cart) {
                val cartQuery = userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(item.id)
                    .get()
                    .await()

                if (cartQuery.data != null) {
                    item.subTotal = item.calculatedTotalPrice.toDouble()
                    val updateCartQtyMap = mapOf<String, Any>(
                        "quantity" to item.quantity,
                        "subTotal" to item.subTotal
                    )

                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(item.id)
                        .update(updateCartQtyMap)
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                    return@withContext isCompleted
                }
            }
            false
        }
        return isSuccessful
    }

    suspend fun delete(
        userId: String,
        cartId: String
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            val cartItemQuery = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartId)
                .get()
                .await()

            if (cartItemQuery.data != null) {
                userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(cartId)
                    .delete()
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
                return@withContext isCompleted
            }
            false
        }
        return isSuccessful
    }

    suspend fun deleteOutOfStockItems(
        userId: String,
        cartList: List<Cart>
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            for (cartItem in cartList) {
                userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(cartItem.id)
                    .delete()
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    suspend fun deleteAll(
        userId: String
    ): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            val cartQuery = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .get()
                .await()

            if (cartQuery.documents.isNotEmpty()) {
                for (cartItem in cartQuery.documents) {
                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(cartItem.id)
                        .delete()
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }
                return@withContext isCompleted
            }
            false
        }
        return isSuccessful
    }
}
