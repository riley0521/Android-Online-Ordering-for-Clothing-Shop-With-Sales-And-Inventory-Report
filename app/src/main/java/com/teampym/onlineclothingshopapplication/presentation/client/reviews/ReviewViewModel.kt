package com.teampym.onlineclothingshopapplication.presentation.client.reviews

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val reviewRepository: ReviewRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val productId = MutableLiveData("")

    private val _reviews = combine(
        productId.asFlow(),
        preferencesManager.preferencesFlow
    ) { productId, session ->
        Pair(productId, session)
    }.flatMapLatest { (productId, session) ->
        reviewRepository.getSome(
            session.userId,
            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(REVIEWS_SUB_COLLECTION)
                .orderBy("dateReview", Query.Direction.DESCENDING)
                .limit(30)
        ).flow.cachedIn(viewModelScope)
    }

    val reviews = _reviews.asLiveData()
}
