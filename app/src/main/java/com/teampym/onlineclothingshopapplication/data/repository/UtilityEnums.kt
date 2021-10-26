package com.teampym.onlineclothingshopapplication.data.repository

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

enum class ProductFlag {
    NORMAL,
    NEW,
    BEST_SELLER
}

enum class CartFlag {
    ADDING,
    REMOVING
}

enum class UserType {
    CUSTOMER,
    ADMIN
}