package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product

data class ProductWithInventory(
    @Embedded val product: Product,
    @Relation(
        parentColumn = "productId",
        entityColumn = "pid"
    )
    val inventory: Inventory
)

data class CartWithProductAndInventory(
    @Embedded val cart: Cart,
    @Relation(
        entity = Product::class,
        parentColumn = "id",
        entityColumn = "cartId"
    )
    val productWithInventory: ProductWithInventory
)
