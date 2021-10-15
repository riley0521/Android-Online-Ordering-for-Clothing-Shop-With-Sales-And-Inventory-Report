package com.teampym.onlineclothingshopapplication.data.repository

enum class Status {
    SHIPPING,
    SHIPPED,
    DELIVERY,
    COMPLETED,

    // Status for Rider when the user wants to return
    RETURNING,
    RETURNED_TO_RIDER,
    RETURNED_TO_WAREHOUSE,

    // When the user canceled the order within 24 hours
    CANCELED
}

enum class ProductFlag {
    NORMAL,
    NEW,
    BEST_SELLER
}

enum class UserType {
    CUSTOMER,
    ADMIN
}