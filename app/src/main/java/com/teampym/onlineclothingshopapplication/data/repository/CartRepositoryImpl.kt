package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import javax.inject.Inject

class CartRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val userCartCollectionRef = db.collection("Users")
    private val updatedProductsCollectionRef = db.collection("Products")

    suspend fun getCartByUserId(userId: String): List<Cart>? {
        // get specific cart by userId
        val cartQuery = userCartCollectionRef.document(userId).collection("cart").get().await()
        val cartList = mutableListOf<Cart>()
        if (cartQuery != null) {
            for (cartItem in cartQuery.documents) {

                // check if price of a single product has been changed and update the cart instantly.
                val productId = cartItem["product.id"].toString()
                val productQuery = updatedProductsCollectionRef.document(productId).get().await()
                if (productQuery.exists()) {
                    val updatePriceOfProductInCart = mapOf<String, Any>(
                        "price" to productQuery["price"].toString().toBigDecimal()
                    )

                    userCartCollectionRef.document(userId).collection("cart").document(cartItem.id)
                        .set(updatePriceOfProductInCart, SetOptions.merge()).await()
                }

                // check if the inventory stock and committed changes and update the cart instantly.
                val inventoryId = cartItem["selectedSizeFromInventory.id"].toString()
                val inventoryQuery =
                    db.collectionGroup("Inventories").whereEqualTo("id", inventoryId).limit(1).get()
                        .await()
                if (inventoryQuery != null) {
                    val updateStockAndCommittedItemToCart = mapOf<String, Any>(
                        "stock" to inventoryQuery.documents[0]["stock"].toString()
                            .toLong() - inventoryQuery.documents[0]["committed"].toString()
                            .toLong(),
                        "committed" to inventoryQuery.documents[0]["committed"].toString().toLong()
                    )

                    userCartCollectionRef.document(userId).collection("cart").document(cartItem.id)
                        .set(updateStockAndCommittedItemToCart, SetOptions.merge()).await()
                }

                cartList.add(
                    Cart(
                        id = cartItem["id"].toString(),
                        userId = cartItem["userId"].toString(),
                        product = Product(
                            id = cartItem["product.id"].toString(),
                            categoryId = cartItem["product.categoryId"].toString(),
                            name = cartItem["product.name"].toString(),
                            description = cartItem["product.description"].toString(),
                            imageUrl = cartItem["product.imageUrl"].toString(),
                            price = cartItem["product.price"].toString().toBigDecimal(),
                            flag = cartItem["product.flag"].toString(),
                            inventories = null,
                            productImages = null
                        ),
                        quantity = cartItem["quantity"].toString().toLong(),
                        selectedSizeFromInventory = Inventory(
                            id = cartItem["selectedSizeFromInventory.id"].toString(),
                            productId = cartItem["selectedSizeFromInventory.productId"].toString(),
                            size = cartItem["selectedSizeFromInventory.size"].toString(),
                            stock = cartItem["selectedSizeFromInventory.stock"].toString()
                                .toLong() - cartItem["selectedSizeFromInventory.committed"].toString()
                                .toLong(),
                            committed = cartItem["selectedSizeFromInventory.committed"].toString()
                                .toLong(),
                            sold = cartItem["selectedSizeFromInventory.sold"].toString().toLong(),
                            returned = cartItem["selectedSizeFromInventory.returned"].toString()
                                .toLong(),
                            restockLevel = cartItem["selectedSizeFromInventory.restockLevel"].toString()
                                .toLong()
                        ),
                        subTotal = cartItem["subTotal"].toString().toBigDecimal()
                    )
                )
            }

            updateAndGetTotalOfCart(userId)
            return cartList
        }
        return null
    }

    suspend fun updateAndGetTotalOfCart(userId: String): BigDecimal {
        // Update the totalOfCart whether the product per price changes or not to add the grand total of all subTotal.
        val currentCart = getCartByUserId(userId)
        if (currentCart != null) {
            val updateTotalOfCart = mapOf<String, Any>(
                "totalOfCart" to currentCart.sumOf { it.subTotal }
            )
            val result =
                userCartCollectionRef.document(userId).set(updateTotalOfCart, SetOptions.merge())
                    .await()
            if (result != null) {
                // TODO("Maybe I can cache the grandTotal in room database here in repository using DAO")
                return currentCart.sumOf { it.subTotal }
            }
        }
        return "0".toBigDecimal()
    }

    suspend fun addToCart(
        userId: String,
        product: Product,
        inventory: Inventory,
        quantity: Long
    ): Boolean {
        // get specific cart by userId
        val cartQuery = userCartCollectionRef.document(userId).collection("cart").get().await()
        if (cartQuery != null) {
            if (cartQuery.size() == 10)
                return false

            // loop in the cart collection to check if item is existing in the cart
            for (cartItem in cartQuery.documents) {

                if (cartItem.id == product.id) {
                    val updateQuantityOfItemInCart = mapOf<String, Any>(
                        "quantity" to cartItem["quantity"].toString().toLong() + quantity
                    )

                    val result = userCartCollectionRef.document(userId).collection("cart")
                        .document(cartItem.id).set(updateQuantityOfItemInCart, SetOptions.merge())
                        .await()
                    return result != null
                }
            }

            val newItemInCart = Cart(
                id = product.id,
                userId = userId,
                product = product,
                quantity = quantity,
                selectedSizeFromInventory = inventory,
                subTotal = product.price * quantity.toBigDecimal()
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
        val cartItemQuery =
            userCartCollectionRef.document(userId).collection("cart").document(cartId).get().await()
        if (cartItemQuery != null) {
            val newQuantity = when (flag) {
                CartFlag.ADDING.toString() -> cartItemQuery["quantity"].toString().toLong() + 1
                CartFlag.REMOVING.toString() -> cartItemQuery["quantity"].toString().toLong() - 1
                else -> 0L
            }

            val updateQuantityOfCartItem = mapOf<String, Any>(
                "quantity" to newQuantity,
                "subTotal" to (cartItemQuery["product.price"].toString()
                    .toBigDecimal() * newQuantity.toBigDecimal())
            )

            val result = userCartCollectionRef.document(userId).collection("cart").document(cartId)
                .set(updateQuantityOfCartItem, SetOptions.merge()).await()
            updateAndGetTotalOfCart(userId)
            return result != null
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
        if(cartQuery != null) {
            for(cartItem in cartQuery.documents) {
                userCartCollectionRef.document(userId).collection("cart").document(cartItem.id).delete().await()
            }
            return true
        }
        return false
    }
}