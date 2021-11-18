package com.teampym.onlineclothingshopapplication.data.repository

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
    private val db: FirebaseFirestore,
    private val cartDao: CartDao,
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao
) {

    private val userCartCollectionRef = db.collection(USERS_COLLECTION)
    private val productsCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Use a flow to get updates in cart collection instantly")
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
                                val productQuery = productsCollectionRef
                                    .document(cartItem.product.productId)
                                    .get()
                                    .await()

                                if (productQuery.data != null) {

                                    val updatedProductToFetch = productQuery.toObject<Product>()!!
                                        .copy(
                                            productId = productQuery.id,
                                            roomId = cartItem.id,
                                            cartId = cartItem.id
                                        )

                                    // get corresponding inventory of a single product (product.id == inventory.productId)
                                    val inventoryQuery = productsCollectionRef
                                        .document(updatedProductToFetch.productId)
                                        .collection(INVENTORIES_SUB_COLLECTION)
                                        .document(cartItem.inventory.inventoryId)
                                        .get()
                                        .await()

                                    if (inventoryQuery.data != null) {
                                        val updatedInventoryToFetch =
                                            inventoryQuery.toObject<Inventory>()!!
                                                .copy(
                                                    inventoryId = inventoryQuery.id,
                                                    pid = cartItem.product.productId,
                                                    pCartId = cartItem.id
                                                )

                                        // create update map.
                                        val updatePriceOfProductInCart = mapOf<String, Any>(
                                            "product.price" to updatedProductToFetch.price,
                                            "inventory.stock" to updatedInventoryToFetch.stock,
                                            "subTotal" to cartItem.quantity * updatedProductToFetch.price,
                                        )

                                        userCartCollectionRef
                                            .document(userId)
                                            .collection(CART_SUB_COLLECTION)
                                            .document(document.id)
                                            .update(updatePriceOfProductInCart)
                                            .addOnSuccessListener {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    // TODO("INSERT PRODUCT AND INVENTORY TO ROOM DB BECAUSE IT IS NOT LOADING IN CHECK OUT")
                                                    cartItem = cartItem.copy(
                                                        product = updatedProductToFetch,
                                                        inventory = updatedInventoryToFetch
                                                    )

                                                    productDao.insert(updatedProductToFetch)
                                                    inventoryDao.insert(updatedInventoryToFetch)
                                                }
                                            }.addOnFailureListener {
                                            }
                                    }
                                }
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

    suspend fun insert(
        userId: String,
        product: Product,
        inventory: Inventory
    ): Resource {

        val cartQuery = userCartCollectionRef
            .document(userId)
            .collection("cart")
            .document(inventory.inventoryId)
            .get()
            .await()

        // check if the productId is existing in the user's cart. if true then update, if false then just add it.
        if (cartQuery.data != null) {
            val cartItem = cartQuery.toObject(Cart::class.java)?.copy(id = cartQuery.id)

            cartItem?.let { obj ->
                if (obj.inventory.inventoryId == inventory.inventoryId && obj.userId == userId) {

                    obj.quantity += 1
                    // every time you get calculatedTotalPrice in Cart data class.
                    // it will calculate quantity * product.price
                    obj.subTotal = obj.calculatedTotalPrice.toDouble()
                    val updateQuantityOfItemInCart = mapOf<String, Any>(
                        "quantity" to obj.quantity,
                        "subTotal" to obj.subTotal
                    )

                    var isUpdated = false
                    userCartCollectionRef
                        .document(userId)
                        .collection(CART_SUB_COLLECTION)
                        .document(obj.id)
                        .set(updateQuantityOfItemInCart, SetOptions.merge())
                        .addOnSuccessListener {
                            isUpdated = true
                            CoroutineScope(Dispatchers.IO).launch {
                                cartDao.insert(obj)
                            }
                        }.addOnFailureListener {
                        }
                    return Resource.Success("Success", isUpdated)
                }
            }
        } else {
            val newItem = Cart(
                id = inventory.inventoryId,
                userId = userId,
                product = product,
                quantity = 1,
                inventory = inventory,
                subTotal = 0.0
            )
            newItem.subTotal = newItem.calculatedTotalPrice.toDouble()

            var isCreated = false
            userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(inventory.inventoryId)
                .set(newItem)
                .addOnSuccessListener {
                    isCreated = true
                    CoroutineScope(Dispatchers.IO).launch {
                        cartDao.insert(newItem)
                    }
                }.addOnFailureListener {
                }
            return Resource.Success("Success", isCreated)
        }
        return Resource.Error("Failed", false)
    }

    suspend fun update(
        userId: String,
        cart: List<Cart>
    ): Boolean {

        var isUpdated = false
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
                        isUpdated = true
                    }.addOnFailureListener {
                    }
            }
        }
        return isUpdated
    }

    suspend fun delete(
        userId: String,
        cartId: String
    ): Resource {
        val cartItemQuery = userCartCollectionRef
            .document(userId)
            .collection(CART_SUB_COLLECTION)
            .document(cartId)
            .get()
            .await()

        if (cartItemQuery.data != null) {
            var isDeleted = false
            userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartId)
                .delete()
                .addOnSuccessListener {
                    isDeleted = true
                }.addOnFailureListener {
                }
            return Resource.Success("Success", isDeleted)
        }
        return Resource.Error("Failed", false)
    }

    suspend fun deleteOutOfStockItems(
        userId: String,
        cartList: List<Cart>
    ): Boolean {

        for (cartItem in cartList) {
            userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(cartItem.id)
                .delete()
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        cartDao.delete(cartItem.id)
                    }
                }.addOnFailureListener {
                }
        }
        return true
    }

    suspend fun deleteAll(
        userId: String
    ): Resource {
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
                    }
            }
            cartDao.deleteAll(userId)
            return Resource.Success("Success", true)
        }
        return Resource.Error("Failed", false)
    }
}
