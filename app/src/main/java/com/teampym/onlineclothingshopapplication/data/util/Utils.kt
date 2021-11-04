package com.teampym.onlineclothingshopapplication.data.util

const val CATEGORIES_COLLECTION = "Categories"

const val PRODUCTS_COLLECTION = "Products"
const val PRODUCT_IMAGES_SUB_COLLECTION = "productImages"
const val INVENTORIES_SUB_COLLECTION = "inventories"
const val REVIEWS_SUB_COLLECTION = "reviews"

const val USERS_COLLECTION = "Users"
const val DELIVERY_INFORMATION_SUB_COLLECTION = "deliveryInformation"
const val NOTIFICATION_TOKENS_SUB_COLLECTION = "notificationTokens"
const val CART_SUB_COLLECTION = "cart"

const val ORDERS_COLLECTION = "Orders"
const val ORDER_DETAILS_SUB_COLLECTION = "orderDetails"

const val POSTS_COLLECTION = "Posts"
const val LIKES_SUB_COLLECTION = "likes"
const val COMMENTS_SUB_COLLECTION = "comments"

class Utils {
    companion object {
        var productFlag: String = ""
        var userId: String = ""
    }
}

sealed class Resource {
    data class Success(val msg: String, val res: Any): Resource()
    data class Error(val msg: String, val ex: Any?): Resource()
}

enum class Status {
    SHIPPING,
    SHIPPED,
    DELIVERY,
    COMPLETED,
    // When the item have defect
    RETURNED,
    // When the user canceled the order within 24 hours
    CANCELED
}

enum class PaymentMethod {
    GCASH,
    PAYMAYA,
    BPI,
    COD
}

enum class CartFlag {
    ADDING,
    REMOVING
}

enum class UserType {
    CUSTOMER,
    ADMIN
}

enum class ProductType {
    SHIRTS,
    HOODIES
}

enum class LogType {
    CATEGORY,
    PRODUCT,
    PRODUCT_IMAGE,
    INVENTORY,
    ORDER,
    POST
}