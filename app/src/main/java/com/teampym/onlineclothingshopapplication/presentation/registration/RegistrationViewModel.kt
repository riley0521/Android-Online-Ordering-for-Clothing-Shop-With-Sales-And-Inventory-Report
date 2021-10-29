package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountAndDeliveryInformationImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val accountRepository: AccountAndDeliveryInformationImpl
) : ViewModel() {

    private val registrationEventChannel = Channel<RegistrationEvent>()
    val registrationEvent = registrationEventChannel.receiveAsFlow()

    fun registerUser(
        firstName: String,
        lastName: String,
        birthDate: String,
        user: FirebaseUser
    ) = viewModelScope.launch {
            accountRepository.createUser(
                user.uid,
                firstName,
                lastName,
                birthDate,
                user.photoUrl?.toString() ?: ""
            )
        registrationEventChannel.send(RegistrationEvent.SuccessfulEvent("Registered user successfully!"))
    }

    fun updateBasicInformation(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ) = viewModelScope.launch {
        accountRepository.updateUserBasicInformation(userId, firstName, lastName, birthDate)
    }

    sealed class RegistrationEvent {
        data class SuccessfulEvent(val msg: String) : RegistrationEvent()
    }
}