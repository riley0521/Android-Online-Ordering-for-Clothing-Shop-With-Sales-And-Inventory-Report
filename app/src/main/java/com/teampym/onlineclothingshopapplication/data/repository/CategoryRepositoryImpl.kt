package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    fun getCategories(): CollectionReference {
        return db.collection("Categories")
    }

}