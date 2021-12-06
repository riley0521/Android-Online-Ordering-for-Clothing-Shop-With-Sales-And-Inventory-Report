package com.teampym.onlineclothingshopapplication.data.room

import androidx.room.Embedded
import androidx.room.Relation
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.models.WishItem

data class UserWithDeliveryInfo(
    @Embedded val user: UserInformation,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val deliveryInformation: List<DeliveryInformation>
)

data class UserWithNotificationTokens(
    @Embedded val user: UserInformation,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val notificationTokens: List<NotificationToken>
)

data class UserWithWishList(
    @Embedded val user: UserInformation?,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userId"
    )
    val wishList: List<WishItem>
)
