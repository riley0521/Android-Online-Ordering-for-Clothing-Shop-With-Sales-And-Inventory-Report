package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.db.CartDao
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import com.teampym.onlineclothingshopapplication.data.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val cartDao: CartDao
) {

    private val userCartCollectionRef = db.collection(USERS_COLLECTION)
    private val productsCollectionRef = db.collection(PRODUCTS_COLLECTION)


    // TODO("Use a flow to get updates in cart collection instantly")
    fun getAll(userId: String): Flow<MutableList<Cart>> = callbackFlow {

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
                        val cartItem = document.toObject(Cart::class.java)!!.copy(id = document.id)

                        CoroutineScope(Dispatchers.IO).launch {

                            // get corresponding product (cartItem.id == product.id)
                            val productQuery = productsCollectionRef
                                .document(cartItem.product.id)
                                .get()
                                .await()

                            if (productQuery.data != null) {

                                val pr = productQuery.toObject<Product>()!!
                                    .copy(id = productQuery.id)

                                // get corresponding inventory of a single product (product.id == inventory.productId)
                                val inventoryQuery = productsCollectionRef
                                    .document(pr.id)
                                    .collection(INVENTORIES_SUB_COLLECTION)
                                    .document(cartItem.sizeInv.id)
                                    .get()
                                    .await()

                                if(inventoryQuery.data != null) {
                                    val inv = inventoryQuery.toObject<Inventory>()!!
                                        .copy(id = inventoryQuery.id, productId = cartItem.product.id)

                                    // create update map.
                                    val updatePriceOfProductInCart = mapOf<String, Any>(
                                        "product.price" to pr.price,
                                        "sizeInv.stock" to inv.stock,
                                        "subTotal" to cartItem.quantity * pr.price,
                                    )

                                    userCartCollectionRef
                                        .document(userId)
                                        .collection(CART_SUB_COLLECTION)
                                        .document(document.id)
                                        .update(updatePriceOfProductInCart)
                                        .addOnSuccessListener {

                                        }.addOnFailureListener {

                                        }
                                }

                            }
                        }
                        cartList.add(cartItem)
                    }
                }
                offer(cartList)
            }
        awaitClose {
            cartListener.remove()
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
            .document(product.id)
            .get()
            .await()

        // check if the productId is existing in the user's cart. if true then update, if false then just add it.
        if (cartQuery.data != null) {
            val cartItem = cartQuery.toObject(Cart::class.java)!!.copy(id = cartQuery.id)

            cartItem.let { obj ->
                if (obj.product.id == product.id && obj.userId == userId) {

                    obj.quantity += 1
                    val updateQuantityOfItemInCart = mapOf<String, Any>(
                        "quantity" to obj.quantity
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
                id = product.id,
                userId = userId,
                product = product,
                quantity = 1,
                sizeInv = inventory,
                subTotal = 0.0
            )
            newItem.subTotal = newItem.calculatedTotalPrice.toDouble()

            var isCreated = false
            userCartCollectionRef.document(userId).collection(CART_SUB_COLLECTION)
                .document(product.id)
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
        for(item in cart) {
            val cartQuery = userCartCollectionRef
                .document(userId)
                .collection(CART_SUB_COLLECTION)
                .document(item.id)
                .get()
                .await()

            if(cartQuery.data != null) {
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
            return Resource.Success("Success", true)
        }
        return Resource.Error("Failed", false)
    }
}