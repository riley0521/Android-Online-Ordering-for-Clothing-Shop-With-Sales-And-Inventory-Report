package com.teampym.onlineclothingshopapplication.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val userInformationDao: UserInformationDao
) {

    private val userCartCollectionRef = db.collection("Users")
    private val updatedProductsCollectionRef = db.collection("Products")


    // TODO("Use a flow to get updates in cart collection instantly")
    fun getCartByUserId(userId: String): Flow<List<Cart>> {

        return callbackFlow {
            val cartListener = userCartCollectionRef.document(userId)
                .collection("cart")
                .addSnapshotListener { querySnapshot, firebaseException ->
                    if (firebaseException != null) {
                        cancel(message = "Error fetching cart", firebaseException)
                        return@addSnapshotListener
                    }

                    val cartList = mutableListOf<Cart>()
                    querySnapshot!!.documents.mapNotNull { document ->
                        val cartItem = document.toObject(Cart::class.java)
                        cartItem?.let {

                            CoroutineScope(Dispatchers.IO).launch {
                                val productId = it.productId
                                val productQuery =
                                    updatedProductsCollectionRef.document(productId).get().await()
                                if (productQuery.exists()) {
                                    val updatePriceOfProductInCart = mapOf<String, Any>(
                                        "price" to productQuery["price"].toString().toBigDecimal()
                                    )

                                    userCartCollectionRef.document(userId).collection("cart")
                                        .document(document.id)
                                        .set(updatePriceOfProductInCart, SetOptions.merge()).await()
                                }
                            }
                            cartList.add(it)
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        updateTotalOfCart(userId)
                    }
                    offer(cartList)
                }

            awaitClose {
                Log.d("CART LISTENER", "Removing cart listener")
                cartListener.remove()
            }
        }

//        // get specific cart by userId
//        val cartQuery = userCartCollectionRef.document(userId).collection("cart").get().await()
//        val cartList = mutableListOf<Cart>()
//        if (cartQuery != null) {
//            for (document in cartQuery.documents) {
//
//                val cartItem = document.toObject(Cart::class.java)
//
//                cartItem?.let {
//                    // check if price of a single product has been changed and update the cart instantly.
//                    val productId = it.productId
//                    val productQuery =
//                        updatedProductsCollectionRef.document(productId).get().await()
//                    if (productQuery.exists()) {
//                        val updatePriceOfProductInCart = mapOf<String, Any>(
//                            "price" to productQuery["price"].toString().toBigDecimal()
//                        )
//
//                        userCartCollectionRef.document(userId).collection("cart")
//                            .document(document.id)
//                            .set(updatePriceOfProductInCart, SetOptions.merge()).await()
//                    }
//                    cartList.add(it)
//                }
//            }
//            updateTotalOfCart(userId)
//            return cartList
//        }
//        return null
    }

    suspend fun updateTotalOfCart(userId: String): Double {
        // Update the totalOfCart whether the product per price changes or not to add the grand total of all subTotal.
        val userCartQuery = userCartCollectionRef.document(userId).collection("cart").get().await()

        if (userCartQuery != null) {

            val cartList = mutableListOf<Cart>()
            for (document in userCartQuery.documents) {
                val cartItem = document.toObject(Cart::class.java)
                cartItem?.let {
                    cartList.add(it)
                }
            }

            val updateTotalOfCart = mapOf<String, Any>(
                "totalOfCart" to cartList.sumOf { it.subTotal }
            )
            val result = userCartCollectionRef.document(userId)
                .set(updateTotalOfCart, SetOptions.merge())
                .await()
            if (result != null) {
                val totalOfCart = cartList.sumOf { it.subTotal }
                userInformationDao.updateTotalOfCart(userId, totalOfCart)
                return totalOfCart
            }
        }
        return 0.0
    }

    suspend fun addToCart(
        userId: String,
        product: Product,
        inventory: Inventory,
        quantity: Long
    ): Boolean {

        val cartQuery =
            userCartCollectionRef.document(userId).collection("cart").document(product.id).get()
                .await()

        // check if the productId is existing in the user's cart. if true then update, if false then just add it.
        if (cartQuery != null) {

            val obj = cartQuery.toObject(Cart::class.java)

            obj?.let { cart ->
                if (cart.id == product.id) {
                    val updateQuantityOfItemInCart = mapOf<String, Any>(
                        "quantity" to cart.quantity + quantity
                    )

                    val result = userCartCollectionRef.document(userId).collection("cart")
                        .document(cart.id).set(updateQuantityOfItemInCart, SetOptions.merge())
                        .await()
                    return result != null
                }
            }
        } else {
            val newItemInCart = Cart(
                id = product.id,
                userId = userId,
                productId = product.id,
                inventoryId = inventory.id,
                product = product,
                quantity = quantity,
                sizeInv = inventory,
                subTotal = product.price * quantity.toDouble()
            )

            val result = userCartCollectionRef.document(userId).collection("cart")
                .document(product.id)
                .set(newItemInCart)
                .await()
            return result != null
        }
        return false
    }

    suspend fun updateCartQuantity(
        userId: String,
        cartId: String,
        flag: String,
    ): Boolean {
        val updateCartQtyQuery =
            userCartCollectionRef.document(userId).collection("cart").document(cartId).get().await()

        if (updateCartQtyQuery != null) {

            val cartItemToUpdate = updateCartQtyQuery.toObject(Cart::class.java)

            cartItemToUpdate?.let {
                val newQuantity = when (flag) {
                    CartFlag.ADDING.toString() -> it.quantity + 1
                    CartFlag.REMOVING.toString() -> it.quantity - 1
                    else -> 0L
                }

                val updateQuantityOfCartItem = mapOf<String, Any>(
                    "quantity" to newQuantity,
                    "subTotal" to (it.product.price * newQuantity.toDouble())
                )

                val result =
                    userCartCollectionRef.document(userId).collection("cart").document(cartId)
                        .set(updateQuantityOfCartItem, SetOptions.merge()).await()
                updateTotalOfCart(userId)
                return result != null

            }
        }
        return false
    }

    suspend fun deleteCartItem(
        userId: String,
        cartId: String
    ): Boolean {
        val cartItemQuery =
            userCartCollectionRef.document(userId).collection("cart").document(cartId).get().await()
        if (cartItemQuery != null) {
            val result =
                userCartCollectionRef.document(userId).collection("cart").document(cartId).delete()
                    .await()
            return result != null
        }
        return false
    }

    suspend fun deleteAllItemFromCart(
        userId: String
    ): Boolean {
        val cartQuery = userCartCollectionRef.document(userId).collection("cart").get().await()
        if (cartQuery != null) {
            for (cartItem in cartQuery.documents) {
                userCartCollectionRef.document(userId).collection("cart").document(cartItem.id)
                    .delete().await()
            }
            return true
        }
        return false
    }
}