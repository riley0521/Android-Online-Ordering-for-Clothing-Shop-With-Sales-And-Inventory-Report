package com.teampym.onlineclothingshopapplication.data.db

import androidx.room.Embedded
import androidx.room.Relation
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.UserInformation


data class UserWithDeliveryInfo(
    @Embedded val user: UserInformation,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val deliveryInformation: List<DeliveryInformation>
)

data class UserWithTokens(
    @Embedded val user: UserInformation,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val notificationTokens: List<NotificationToken>
)

data class UserWithCart(
    @Embedded val user: UserInformation,
    @Relation(
        parentColumn = "id",
        entityColumn = "userId"
    )
    val cart: List<Cart>
)