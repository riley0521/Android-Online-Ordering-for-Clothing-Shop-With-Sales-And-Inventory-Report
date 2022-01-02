package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    var title: String,
    var description: String,
    var createdBy: String,
    var id: String = "",
    var avatarUrl: String = "",
    var imageUrl: String = "",
    var dateCreated: Long = 0,
    var numberOfLikes: Long = 0,
    var numberOfComments: Long = 0
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        ""
    )

    @get:Exclude
    var isLikedByCurrentUser: Boolean = false

    @get:Exclude
    var haveUserId: Boolean = false
}
