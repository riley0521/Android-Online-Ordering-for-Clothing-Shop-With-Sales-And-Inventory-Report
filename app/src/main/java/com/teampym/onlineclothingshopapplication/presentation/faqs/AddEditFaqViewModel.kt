package com.teampym.onlineclothingshopapplication.presentation.faqs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.FAQModel
import com.teampym.onlineclothingshopapplication.data.repository.FAQRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditFaqViewModel @Inject constructor(
    private val faqRepository: FAQRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val QUESTION = "question"
        const val ANSWER = "answer"
    }

    var question = state.get(QUESTION) ?: ""
        set(value) {
            field = value
            state.set(QUESTION, value)
        }

    var answer = state.get(ANSWER) ?: ""
        set(value) {
            field = value
            state.set(ANSWER, value)
        }

    private val _addEditFaqChannel = Channel<AddEditFaqEvent>()
    val addEditFaqEvent = _addEditFaqChannel.receiveAsFlow()

    private fun resetUiState() {
        question = ""
        answer = ""
    }

    private fun isFormValid(): Boolean {
        return question.isNotBlank() && answer.isNotBlank()
    }

    fun onSubmitClicked(faq: FAQModel?, isEditMode: Boolean) = viewModelScope.launch {
        if (faq != null && isEditMode) {
            if (isFormValid()) {
                val res = faqRepository.update(faq.copy(question = question, answer = answer))
                if (res) {
                    resetUiState()
                    _addEditFaqChannel.send(AddEditFaqEvent.NavigateBackWithMessage("FAQ updated Successfully!"))
                } else {
                    _addEditFaqChannel.send(AddEditFaqEvent.ShowErrorMessage("Updating FAQ failed. Please try again."))
                }
            } else {
                _addEditFaqChannel.send(AddEditFaqEvent.ShowErrorMessage("Please fill the form."))
            }
        } else {
            if (isFormValid()) {
                val res = faqRepository.create(
                    FAQModel(
                        question = question,
                        answer = answer
                    )
                )
                if (res != null) {
                    resetUiState()
                    _addEditFaqChannel.send(AddEditFaqEvent.NavigateBackWithMessage("FAQ created Successfully!"))
                } else {
                    _addEditFaqChannel.send(AddEditFaqEvent.ShowErrorMessage("Creating FAQ failed. Please try again."))
                }
            } else {
                _addEditFaqChannel.send(AddEditFaqEvent.ShowErrorMessage("Please fill the form."))
            }
        }
    }

    sealed class AddEditFaqEvent() {
        data class NavigateBackWithMessage(val msg: String) : AddEditFaqEvent()
        data class ShowErrorMessage(val msg: String) : AddEditFaqEvent()
    }
}
