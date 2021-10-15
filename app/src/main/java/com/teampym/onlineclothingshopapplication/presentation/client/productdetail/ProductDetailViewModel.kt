package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val db: FirebaseFirestore
): ViewModel() {

    var reviewsFlow = emptyFlow<PagingData<Review>>()

    fun provideQuery(productId: String) {
        val queryReviews = db.collection("Products")
            .document(productId)
            .collection("Reviews")
            .limit(30)

        reviewsFlow = Pager(
            PagingConfig(
                pageSize = 30
            )
        ) {
            ReviewPagingSource(queryReviews)
        }.flow
    }

}