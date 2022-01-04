package com.teampym.onlineclothingshopapplication.presentation.client.reviews

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.repository.ReviewRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.SessionPreferences
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val reviewRepository: ReviewRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var _reviews: Flow<PagingData<Review>>? = null
    val reviews: LiveData<PagingData<Review>>? get() = _reviews?.asLiveData()

    var userSession: SessionPreferences? = null
        private set

    suspend fun fetchUserSessionAndReviews(productId: String) {
        userSession = preferencesManager.preferencesFlow.first()

        _reviews = reviewRepository.getSome(
            userSession?.userId,
            db.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .collection(REVIEWS_SUB_COLLECTION)
                .orderBy("dateReview", Query.Direction.DESCENDING)
                .limit(30)
        ).flow.cachedIn(viewModelScope)
    }
}
