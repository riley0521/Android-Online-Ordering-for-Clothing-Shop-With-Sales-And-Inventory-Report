package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl
) : ViewModel() {

    private val registrationEventChannel = Channel<RegistrationEvent>()
    val registrationEvent = registrationEventChannel.receiveAsFlow()

    fun registerUser(
        firstName: String,
        lastName: String,
        birthDate: String,
        user: FirebaseUser
    ) = viewModelScope.launch {
            accountRepository.create(
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
        accountRepository.update(userId, firstName, lastName, birthDate)
    }

    sealed class RegistrationEvent {
        data class SuccessfulEvent(val msg: String) : RegistrationEvent()
    }
}