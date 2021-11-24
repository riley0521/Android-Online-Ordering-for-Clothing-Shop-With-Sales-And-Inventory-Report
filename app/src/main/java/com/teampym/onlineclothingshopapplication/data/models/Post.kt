package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    var title: String,
    var description: String,
    var createdBy: String,
    var avatarUrl: String?,
    var imageUrl: String?,
    var id: String = "",
    var dateCreated: Long = System.currentTimeMillis(),
    var numberOfLikes: Long = 0,
    var numberOfComments: Long = 0,
    @get:Exclude
    var likeList: List<Like> = emptyList(),
    @get:Exclude
    var commentList: List<Comment> = emptyList()
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        "",
        ""
    )
}
