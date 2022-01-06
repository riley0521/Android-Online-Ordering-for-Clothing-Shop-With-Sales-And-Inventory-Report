package com.teampym.onlineclothingshopapplication.presentation.client.addreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddReviewViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val state: SavedStateHandle
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

    private val _addReviewChannel = Channel<AddReviewEvent>()
    val addReviewEvent = _addReviewChannel.receiveAsFlow()

    fun onSubmitClicked(
        orderDetail: OrderDetail,
        userInfo: UserInformation
    ) = viewModelScope.launch {
        val result = productRepository.submitReview(
            userInfo,
            ratingValue.toDouble(),
            feedbackValue,
            orderDetail
        )
        if (result != null) {
            _addReviewChannel.send(AddReviewEvent.NavigateBackWithResult(true))
        } else {
            _addReviewChannel.send(AddReviewEvent.ShowErrorMessage("Submitting review failed. Please try again."))
        }
    }

    sealed class AddReviewEvent {
        data class NavigateBackWithResult(val isSuccess: Boolean) : AddReviewEvent()
        data class ShowErrorMessage(val msg: String) : AddReviewEvent()
    }
}
