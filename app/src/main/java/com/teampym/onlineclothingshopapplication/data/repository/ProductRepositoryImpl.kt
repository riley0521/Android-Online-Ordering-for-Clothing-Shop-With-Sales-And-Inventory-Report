package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.presentation.client.products.ProductPagingSource
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    // I will create functions for reading, inserting, updating, and deleting products

    fun getProductsPagingSource(queryProducts: Query) =
        Pager(
            PagingConfig(
                pageSize = 30
            )
        ) {
            ProductPagingSource(queryProducts)
        }

}