package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _registrationEventChannel = Channel<RegistrationEvent>()
    val registrationEvent = _registrationEventChannel.receiveAsFlow()

    val userSession = preferencesManager.preferencesFlow.asLiveData()

    private val _user = MutableLiveData<UserInformation>()
    val user: LiveData<UserInformation> get() = _user

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
        )?.let {
            _registrationEventChannel.send(RegistrationEvent.ShowSuccessfulMessage("Created user successfully!"))
        }
    }

    fun fetchNotificationTokensAndWishList(userId: String) = viewModelScope.launch {
        val userWithNotificationTokens = userInformationDao.getUserWithNotificationTokens()
            .firstOrNull { it.user.userId == userId }
        val userWithWishList = userInformationDao.getUserWithWishList()
            .firstOrNull { it.user.userId == userId }
        _user.value = userWithNotificationTokens?.user?.copy(
            notificationTokenList = userWithNotificationTokens.notificationTokens,
            wishList = userWithWishList?.wishList ?: emptyList()
        )
    }

    fun updateBasicInformation(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String
    ) = viewModelScope.launch {
        if (accountRepository.update(userId, firstName, lastName, birthDate)) {
            _registrationEventChannel.send(RegistrationEvent.ShowSuccessfulMessage("Updated user successfully!"))
        } else {
            _registrationEventChannel.send(RegistrationEvent.ShowErrorMessage("Failed to update user. Please try again later."))
        }
    }

    sealed class RegistrationEvent {
        data class ShowSuccessfulMessage(val msg: String) : RegistrationEvent()
        data class ShowErrorMessage(val msg: String) : RegistrationEvent()
    }
}
