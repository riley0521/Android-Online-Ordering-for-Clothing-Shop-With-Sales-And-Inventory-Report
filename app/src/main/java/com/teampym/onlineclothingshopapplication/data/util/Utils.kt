package com.teampym.onlineclothingshopapplication.data.util

import android.content.Intent
import androidx.fragment.app.Fragment
import java.util.*

const val CATEGORIES_COLLECTION = "Categories"

const val PRODUCTS_COLLECTION = "Products"
const val PRODUCT_IMAGES_SUB_COLLECTION = "productImages"
const val INVENTORIES_SUB_COLLECTION = "inventories"
const val REVIEWS_SUB_COLLECTION = "reviews"

const val USERS_COLLECTION = "Users"
const val DELIVERY_INFORMATION_SUB_COLLECTION = "deliveryInformation"
const val NOTIFICATION_TOKENS_SUB_COLLECTION = "notificationTokens"
const val CART_SUB_COLLECTION = "cart"
const val WISH_LIST_SUB_COLLECTION = "wishList"

const val ORDERS_COLLECTION = "Orders"
const val ORDER_DETAILS_SUB_COLLECTION = "orderDetails"

const val POSTS_COLLECTION = "Posts"
const val LIKES_SUB_COLLECTION = "likes"
const val COMMENTS_SUB_COLLECTION = "comments"

const val AUDIT_TRAILS_COLLECTION = "AuditTrails"

const val SALES_COLLECTION = "Sales"
const val MONTHS_SUB_COLLECTION = "months"
const val DAYS_SUB_COLLECTION = "days"

const val PREFIX = "https://midnightmares.page.link"

const val CANCEL_BUTTON = "Cancel"
const val SUGGEST_BUTTON = "Suggest Shipping Fee"
const val CANCEL_OR_SUGGEST = "Cancel | Suggest Shipping Fee"
const val AGREE_TO_SHIPPING_FEE = "Agree To Shipping Fee"
const val ORDER_COMPLETED = "Complete Order"

const val EDIT_BUTTON = "Edit"
const val REMOVE_BUTTON = "Remove"

const val SELECT_MULTIPLE_ADDITIONAL_IMAGES = "Select Multiple Additional Images"
const val DELETE_ALL_ADDITIONAL_IMAGES = "Delete All And Upload New Images"

const val SHIPPING_ORDERS = "Shipping Orders"
const val SHIPPED_ORDERS = "Shipped Orders"
const val DELIVERY_ORDERS = "For Delivery Orders"
const val COMPLETED_ORDERS = "Completed Orders"
const val RETURNED_ORDERS = "Returned Orders"
const val CANCELLED_ORDERS = "Cancelled Orders"

const val ROOT_PATH = "public/"
const val CATEGORY_PATH = "public/categories/"
const val PRODUCT_PATH = "public/products/"

object Utils {
    fun getTimeInMillisUTC(): Long {
        val newDate = Calendar.getInstance()
        newDate.timeZone = TimeZone.getTimeZone("UTC")
        return newDate.timeInMillis
    }
}

fun Fragment.shareDeepLink(deepLink: String, linkType: LinkType) {

    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    when (linkType) {
        LinkType.POST -> {
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "New Post, check it out ->"
            )
        }
        LinkType.PRODUCT -> {
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "New Product, check it out ->"
            )
        }
    }

    intent.putExtra(Intent.EXTRA_TEXT, deepLink)
    if (isAdded) {
        requireContext().startActivity(intent)
    }
}

enum class LinkType {
    POST,
    PRODUCT
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

enum class AuditType {
    CATEGORY,
    PRODUCT,
    INVENTORY,
    STOCK,
    ORDER,
    POST
}
