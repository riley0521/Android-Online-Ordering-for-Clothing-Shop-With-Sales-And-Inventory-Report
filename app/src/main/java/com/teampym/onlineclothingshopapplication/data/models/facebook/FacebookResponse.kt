package com.teampym.onlineclothingshopapplication.data.models.facebook

import com.google.gson.annotations.SerializedName

data class FacebookResponse(
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    val id: String,
    @SerializedName("last_name")
    val lastName: String,
    val picture: Picture
)
