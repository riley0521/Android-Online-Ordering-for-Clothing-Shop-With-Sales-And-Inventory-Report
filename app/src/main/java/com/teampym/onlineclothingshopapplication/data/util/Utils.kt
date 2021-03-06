package com.teampym.onlineclothingshopapplication.data.util

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ktx.androidParameters
import java.text.DateFormatSymbols
import java.util.* // ktlint-disable no-wildcard-imports

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
const val DAYS_SUB_COLLECTION_OF_MONTHS = "days"

const val RETURNS_COLLECTION = "Returns"
const val IMAGES_SUB_COLLECTION = "images"

const val TERMS_DATA = "Terms"
const val SIZE_CHART_DATA = "SizeChart"
const val FAQ_DATA = "Faqs"

const val SHIPPING_FEES_DATA = "ShippingFees"
const val SHIPPING_ID = "K54UYHdL0y0RSc6jOjLJ"

const val PREFIX = "https://midnightmares.page.link"

const val RECEIVED_ORDER = "Received Order"
const val ORDER_COMPLETED_OR_SHOULDERED_BY_ADMIN = "ORDER COMPLETED | SF SHOULDERED BY ADMIN"

const val EDIT_BUTTON = "Edit"
const val REMOVE_PRODUCT_BUTTON = "Remove Product"
const val REMOVE_SIZE_BUTTON = "Remove Size"

const val SELECT_MULTIPLE_ADDITIONAL_IMAGES = "Select Multiple Additional Images"
const val DELETE_ALL_ADDITIONAL_IMAGES = "Delete All And Upload New Images"

const val SHIPPING_ORDERS = "Shipping Orders"
const val SHIPPED_ORDERS = "Shipped Orders"
const val DELIVERY_ORDERS = "For Delivery Orders"
const val COMPLETED_ORDERS = "Completed Orders"
const val CANCELLED_ORDERS = "Cancelled Orders"

const val ROOT_PATH = "public/"
const val CATEGORY_PATH = "public/categories/"
const val PRODUCT_PATH = "public/products/"
const val POST_PATH = "public/posts/"
const val PROOF_PATH = "public/proofs/"

object Utils {
    fun getTimeInMillisUTC(): Long {
        val newDate = Calendar.getInstance()
        newDate.timeZone = TimeZone.getTimeZone("UTC")
        return newDate.timeInMillis
    }

    fun getAvailableYears() = listOf(
        "2022",
        "2021",
        "2020",
        "2019",
        "2018",
        "2017",
        "2016",
        "2015"
    )

    fun getMonthNames() = DateFormatSymbols().months.map { it.uppercase() }

    fun getCurrentMonth(monthIndex: Int): String {
        val dfs = DateFormatSymbols()
        val months = dfs.months

        return months[monthIndex].uppercase()
    }

    fun generateSharingLink(
        title: String,
        deepLink: Uri,
        previewImageLink: Uri,
        getShareableLink: (String) -> Unit = {},
    ) {
        FirebaseDynamicLinks.getInstance().createDynamicLink().run {
            // What is this link parameter? You will get to know when we will actually use this function.
            link = deepLink

            // [domainUriPrefix] will be the domain name you added when setting up Dynamic Links at Firebase Console.
            // You can find it in the Dynamic Links dashboard.
            domainUriPrefix = PREFIX

            // Pass your preview Image Link here;
            setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle(title)
                    .setImageUrl(previewImageLink)
                    .build()
            )

            // Required
            androidParameters {
                build()
            }

            // Finally
            buildShortDynamicLink()
        }.also {
            it.addOnSuccessListener { dynamicLink ->
                // This lambda will be triggered when short link generation is successful

                // Retrieve the newly created dynamic link so that we can use it further for sharing via Intent.
                getShareableLink.invoke(dynamicLink.shortLink.toString())
            }
            it.addOnFailureListener {
                // This lambda will be triggered when short link generation failed due to an exception

                // Handle
                getShareableLink.invoke("None")
            }
        }
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

enum class CartFlag {
    ADDING,
    REMOVING
}

enum class UserType {
    CUSTOMER,
    ADMIN
}

enum class UserStatus {
    ACTIVE,
    BANNED
}

enum class CourierType {
    ADMINS,
    JNT
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

enum class AuditType {
    ACCOUNT,
    CATEGORY,
    PRODUCT,
    INVENTORY,
    ORDER,
}
