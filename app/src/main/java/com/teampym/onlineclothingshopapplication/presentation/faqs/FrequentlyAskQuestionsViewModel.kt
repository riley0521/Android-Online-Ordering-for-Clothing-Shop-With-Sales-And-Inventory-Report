package com.teampym.onlineclothingshopapplication.presentation.faqs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.FAQModel
import com.teampym.onlineclothingshopapplication.data.repository.FAQRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FrequentlyAskQuestionsViewModel @Inject constructor(
    private val faqRepository: FAQRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val userSession = preferencesManager.preferencesFlow

    var userType = ""

    init {
        viewModelScope.launch {
            userType = userSession.first().userType
        }
    }

    private val _faqs = MutableLiveData<List<FAQModel>>()
    val faqs: LiveData<List<FAQModel>> get() = _faqs

    private val _faqChannel = Channel<FAQEvent>()
    val faqEvent = _faqChannel.receiveAsFlow()

    fun fetchFaqs() = viewModelScope.launch {
        _faqs.postValue(faqRepository.getAll())
    }

    fun deleteFaq(faq: FAQModel, position: Int) = viewModelScope.launch {
        val res = faqRepository.delete(faq.id)
        if (res) {
            _faqChannel.send(FAQEvent.ShowSuccessMessageAndNotifyAdapter("Question deleted successfully!", position))
        } else {
            _faqChannel.send(FAQEvent.ShowErrorMessage("Deleting question failed. Please try again."))
        }
    }

    sealed class FAQEvent {
        data class ShowSuccessMessageAndNotifyAdapter(val msg: String, val position: Int) : FAQEvent()
        data class ShowErrorMessage(val msg: String) : FAQEvent()
    }
}
