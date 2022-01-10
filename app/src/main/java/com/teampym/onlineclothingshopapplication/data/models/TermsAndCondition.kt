package com.teampym.onlineclothingshopapplication.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class TermsAndCondition(
    val id: String,
    val tc: String
): Parcelable
