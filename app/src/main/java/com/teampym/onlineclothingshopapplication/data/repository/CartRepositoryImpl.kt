package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.InventoryDao
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.ProductDao
import com.teampym.onlineclothingshopapplication.data.util.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    val cartDao: CartDao,
    val productDao: ProductDao,
    val inventoryDao: InventoryDao
) {

    private val userCartCollectionRef = db.collection(USERS_COLLECTION)
    private val productsCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Use a flow to get updates in cart collection instantly")
    @ExperimentalCoroutinesApi
    fun getAll(userId: String?): Flow<MutableList<Cart>> = callbackFlow {

        var cartListener: ListenerRegistration? = null
        // Add snapshot listener to cart
        if (userId != null) {
            cartListener = userCartCollectionRef.document(userId)
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
                            var cartItem =
                                document.toObject(Cart::class.java)!!.copy(id = document.id)
                            val foundProduct =
                                cartItem.product.copy(roomId = cartItem.id, cartId = cartItem.id)
                            val foundInventory = cartItem.inventory.copy(pCartId = cartItem.id)

                            cartItem.product = foundProduct
                            cartItem.inventory = foundInventory

                            CoroutineScope(Dispatchers.IO).launch {
                                // get corresponding product (cartItem.id == product.id)
                                cartItem = getUpdatedPriceAndStock(cartItem, userId, document)
                            }
                            cartList.add(cartItem)
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        cartDao.insertAll(cartList)
                    }
                    offer(cartList)
                }
        }
        awaitClose {
            cartListener?.remove()
        }
    }

    private suspend fun getUpdatedPriceAndStock(
        cartItem: Cart,
        userId: String?,
        document: DocumentSnapshot
    ): Cart {
        var finalCartItem = cartItem
        val productQuery = productsCollectionRef
            .document(finalCartItem.product.productId)
            .get()
            .await()

        if (productQuery.data != null) {

            val fetchedUpdatedProduct = productQuery.toObject<Product>()!!
                .copy(
                    productId = productQuery.id,
                    roomId = finalCartItem.id,
                    cartId = finalCartItem.id
                )

            // get corresponding inventory of a single product (product.id == inventory.productId)
            val inventoryQuery = productsCollectionRef
                .document(fetchedUpdatedProduct.productId)
                .collection(INVENTORIES_SUB_COLLECTION)
                .document(finalCartItem.inventory.inventoryId)
                .get()
                .await()

            if (inventoryQuery.data != null) {
                val fetchedUpdatedInventory =
                    inventoryQuery.toObject<Inventory>()!!
                        .copy(
                            inventoryId = inventoryQuery.id,
                            pid = finalCartItem.product.productId,
                            pCartId = finalCartItem.id
                        )

                // create update map.
                val updatePriceOfProductInCart = mapOf<String, Any>(
                    "product.price" to fetchedUpdatedProduct.price,
                    "inventory.stock" to fetchedUpdatedInventory.stock,
                    "subTotal" to finalCartItem.quantity * fetchedUpdatedProduct.price,
                )

                userId?.let {
                    userCartCollectionRef
                        .document(it)
                        .collection(CART_SUB_COLLECTION)
                        .document(document.id)
                        .update(updatePriceOfProductInCart)
                        .addOnSuccessListener {
                            CoroutineScope(Dispatchers.IO).launch {
                                // TODO("INSERT PRODUCT AND INVENTORY TO ROOM DB BECAUSE IT IS NOT LOADING IN CHECK OUT")
                                finalCartItem = finalCartItem.copy(
                                    product = fetchedUpdatedProduct,
                                    inventory = fetchedUpdatedInventory
                                )

                                productDao.insert(fetchedUpdatedProduct)
                                inventoryDao.insert(fetchedUpdatedInventory)
                            }
                        }.addOnFailureListener {
                        }
                }
            }
        }
        return finalCartItem
    }

    suspend fun insert(
        userId: String,
        cartItem: Cart
    ): Boolean {

        var isSuccessful = true

        val cartQuery = userCartCollectionRef
            .document(userId)
            .collection(CART_SUB_COLLECTION)
            .document(cartItem.inventory.inventoryId)
            .get()
            .await()

        // check if the productId is existing in the user's cart. if true then update, if false then just add it.
        if (cartQuery.data != null) {
            val cartItemFromDb = cartQuery.toObject(Cart::class.java)?.copy(id = cartQuery.id)

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
                            isSuccessful = false
                            return@addOnFailureListener
                        }
                    return isSuccessful
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
                    isSuccessful = false
                    return@addOnFailureListener
                }
            return isSuccessful
        }
        return isSuccessful
    }

    suspend fun update(
        userId: String,
        cart: List<Cart>
    ): Boolean {

        var isSuccessful = true
        for (item in cart) {
            val cartQuery = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(item.id)
                .get()
                .await()

            if (cartQuery.data != null) {
                val updateCartQty = mapOf<String, Any>(
                    "quantity" to item.quantity,
                    "subTotal" to item.subTotal
                )

                userCartCollectionRef
                    .document(userId)
                    .collection(CART_SUB_COLLECTION)
                    .document(item.id)
                    .update(updateCartQty)
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isSuccessful = false
                        return@addOnFailureListener
                    }
            }
        }
        return isSuccessful
    }

    suspend fun delete(
        userId: String,
        cartId: String
    ): Boolean {
        var isSuccessful = true
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
                    isSuccessful = false
                    return@addOnFailureListener
                }
            return isSuccessful
        }
        return isSuccessful
    }

    suspend fun deleteOutOfStockItems(
        userId: String,
        cartList: List<Cart>
    ): Boolean {

        var isSuccessful = true
        for (cartItem in cartList) {
            userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartItem.id)
                .delete()
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isSuccessful = false
                    return@addOnFailureListener
                }
        }
        return isSuccessful
    }

    suspend fun deleteAll(
        userId: String
    ): Boolean {
        var isSuccessful = true
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
                        isSuccessful = false
                        return@addOnFailureListener
                    }
            }
            return isSuccessful
        }
        return isSuccessful
    }
}
