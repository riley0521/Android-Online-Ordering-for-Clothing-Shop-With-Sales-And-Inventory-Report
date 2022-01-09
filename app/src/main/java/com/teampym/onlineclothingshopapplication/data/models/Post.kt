package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import com.teampym.onlineclothingshopapplication.data.util.Utils
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    var title: String,
    var description: String,
    var createdBy: String,
    var avatarUrl: String,
    var imageUrl: String,
    var fileName: String,
    var userId: String,
    var id: String = "",
    var dateCreated: Long = Utils.getTimeInMillisUTC(),
    var numberOfLikes: Long = 0,
    var numberOfComments: Long = 0,
    @get:Exclude
    var isLikedByCurrentUser: Boolean = false,
    @get:Exclude
    var haveUserId: Boolean = false
) : Parcelable {

    constructor() : this(
        "",
        "",
        "",
        "",
        "",
        "",
        ""
    )
}
