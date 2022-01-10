package com.teampym.onlineclothingshopapplication.presentation.client.toc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.TermsAndCondition
import com.teampym.onlineclothingshopapplication.data.repository.TermsAndConditionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTermsAndConditionViewModel @Inject constructor(
    private val termsAndConditionRepository: TermsAndConditionRepository
) : ViewModel() {

    private val _editTermsChannel = Channel<EditTermsEvent>()
    val editTermsEvent = _editTermsChannel.receiveAsFlow()

    fun onSubmitClicked(termsAndCondition: TermsAndCondition) = viewModelScope.launch {
        val res = termsAndConditionRepository.update(termsAndCondition)
        if (res) {
            _editTermsChannel.send(EditTermsEvent.NavigateBackWithMessage("Terms and Conditions updated successfully!"))
        } else {
            _editTermsChannel.send(EditTermsEvent.ShowErrorMessage("Updating terms and conditions failed. Please try again."))
        }
    }

    sealed class EditTermsEvent {
        data class NavigateBackWithMessage(val msg: String) : EditTermsEvent()
        data class ShowErrorMessage(val msg: String) : EditTermsEvent()
    }
}
