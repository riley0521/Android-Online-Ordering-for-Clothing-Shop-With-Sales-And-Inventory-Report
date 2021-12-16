package com.teampym.onlineclothingshopapplication.presentation.client.addreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReviewViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val state: SavedStateHandle,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    companion object {
        private const val RATING_VALUE = "rating_value"
        private const val FEEDBACK_STR = "feedback"
    }

    var ratingValue = state.get<Float>(RATING_VALUE) ?: 1f
        set(value) {
            field = value
            state.set(RATING_VALUE, value)
        }

    var feedbackValue = state.get(FEEDBACK_STR) ?: ""
        set(value) {
            field = value
            state.set(FEEDBACK_STR, value)
        }

    fun onSubmitClicked(
        orderDetail: OrderDetail,
        userInfo: UserInformation
    ) = appScope.launch {
        productRepository.submitReview(userInfo, ratingValue.toDouble(), feedbackValue, orderDetail)
    }
}
