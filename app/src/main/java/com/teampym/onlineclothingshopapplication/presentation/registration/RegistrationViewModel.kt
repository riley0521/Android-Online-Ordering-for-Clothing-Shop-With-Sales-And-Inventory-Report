package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
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

    private val _createdUser = MutableLiveData<UserInformation>()
    val createdUser: LiveData<UserInformation> get() = _createdUser

    fun registerUser(firstName: String, lastName: String, birthDate: String, user: FirebaseUser) =
        viewModelScope.launch {
            _createdUser.value = accountRepository.createUser(
                user.uid,
                firstName,
                lastName,
                birthDate,
                user.photoUrl?.userInfo ?: "",
                user.email ?: user.phoneNumber
            )
        }

    fun saveInfoToProto(user: UserInformation) = viewModelScope.launch {
        registrationEventChannel.send(RegistrationEvent.SuccessfulEvent("Registered user successfully!"))
    }

    sealed class RegistrationEvent {
        data class SuccessfulEvent(val msg: String) : RegistrationEvent()
    }

}