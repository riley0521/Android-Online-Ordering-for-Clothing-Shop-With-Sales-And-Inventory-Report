package com.teampym.onlineclothingshopapplication.presentation.client.toc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.TermsAndCondition
import com.teampym.onlineclothingshopapplication.data.repository.TermsAndConditionRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TermsAndConditionViewModel @Inject constructor(
    private val termsAndConditionRepository: TermsAndConditionRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val userSession = preferencesManager.preferencesFlow.asLiveData()

    private val _termsAndCondition = MutableLiveData<TermsAndCondition>()
    val termsAndCondition: LiveData<TermsAndCondition> get() = _termsAndCondition

    fun fetchTermsAndCondition() = viewModelScope.launch {
        _termsAndCondition.postValue(termsAndConditionRepository.get())
    }
}
