package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val registrationEventChannel = Channel<RegistrationEvent>()
    val registrationEvent = registrationEventChannel.receiveAsFlow()

    private val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.getUserFlow(sessionPref.userId)
    }

    val user = userFlow.asLiveData()

    fun registerUser(
        firstName: String,
        lastName: String,
        birthDate: String,
        user: FirebaseUser
    ) = viewModelScope.launch {
        when (
            accountRepository.create(
                user.uid,
                firstName,
                lastName,
                birthDate,
                user.photoUrl?.toString() ?: ""
            )
        ) {
            is Resource.Error -> {
                registrationEventChannel.send(RegistrationEvent.ShowErrorMessage("Failed to register user."))
            }
            is Resource.Success -> {
                registrationEventChannel.send(RegistrationEvent.ShowSuccessfulMessage("Registered user successfully!"))
            }
        }
    }

    fun updateBasicInformation(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ) = viewModelScope.launch {
        when (accountRepository.update(userId, firstName, lastName, birthDate)) {
            is Resource.Error -> {
                registrationEventChannel.send(RegistrationEvent.ShowErrorMessage("Failed to update user."))
            }
            is Resource.Success -> {
                registrationEventChannel.send(RegistrationEvent.ShowSuccessfulMessage("Updated user successfully!"))
            }
        }
    }

    sealed class RegistrationEvent {
        data class ShowSuccessfulMessage(val msg: String) : RegistrationEvent()
        data class ShowErrorMessage(val msg: String) : RegistrationEvent()
    }
}
