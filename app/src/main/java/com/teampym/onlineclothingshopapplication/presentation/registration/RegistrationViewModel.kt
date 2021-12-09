package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepository
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val USER_ID = "user_id"
        private const val FIRST_NAME = "first_name"
        private const val LAST_NAME = "last_name"
        private const val BIRTH_DATE = "birth_date"
    }

    var userId = state.get(USER_ID) ?: ""
        set(value) {
            field = value
            state.set(USER_ID, value)
        }

    var firstName = state.get(FIRST_NAME) ?: ""
        set(value) {
            field = value
            state.set(FIRST_NAME, value)
        }

    var lastName = state.get(LAST_NAME) ?: ""
        set(value) {
            field = value
            state.set(LAST_NAME, value)
        }

    var birthDate = state.get(BIRTH_DATE) ?: ""
        set(value) {
            field = value
            state.set(BIRTH_DATE, value)
        }

    private val _registrationEventChannel = Channel<RegistrationEvent>()
    val registrationEvent = _registrationEventChannel.receiveAsFlow()

    private val _user = MutableLiveData<UserInformation>()
    val user: LiveData<UserInformation> get() = _user

    private fun resetAllUiState() {
        firstName = ""
        lastName = ""
        birthDate = ""
    }

    private fun isFormValid(): Boolean {
        return firstName.isNotBlank() &&
            birthDate.isNotBlank()
    }

    fun onSubmitClicked(
        isEditMode: Boolean,
        photoUrl: String
    ) = viewModelScope.launch {
        if (isEditMode) {
            if (isFormValid()) {
                val res = async {
                    accountRepository.update(
                        userId,
                        firstName,
                        lastName,
                        birthDate
                    )
                }.await()
                if (res) {
                    _registrationEventChannel.send(
                        RegistrationEvent.ShowSuccessfulMessage(
                            "Updated user successfully!"
                        )
                    )
                } else {
                    _registrationEventChannel.send(
                        RegistrationEvent.ShowErrorMessage(
                            "Failed to update user. Please try again later."
                        )
                    )
                }
            } else {
                _registrationEventChannel.send(
                    RegistrationEvent.ShowErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        } else {
            if (isFormValid()) {
                val res = async {
                    accountRepository.create(
                        userId,
                        firstName,
                        lastName,
                        birthDate,
                        photoUrl
                    )
                }.await()
                if (res != null) {
                    _registrationEventChannel.send(
                        RegistrationEvent.ShowSuccessfulMessage(
                            "Created user successfully!"
                        )
                    )
                } else {
                    _registrationEventChannel.send(
                        RegistrationEvent.ShowErrorMessage(
                            "Created user failed. Please try again."
                        )
                    )
                }
            } else {
                _registrationEventChannel.send(
                    RegistrationEvent.ShowErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        }
    }

    fun fetchNotificationTokensAndWishList(userId: String) = viewModelScope.launch {
        val userWithWishList = userInformationDao.getUserWithWishList()
            .firstOrNull { it.user?.userId == userId }

        val finalUser = userWithWishList?.user
        finalUser?.wishList = userWithWishList?.wishList ?: emptyList()

        if (finalUser != null) {
            _user.value = finalUser!!
        }
    }

    sealed class RegistrationEvent {
        data class ShowSuccessfulMessage(val msg: String) : RegistrationEvent()
        data class ShowErrorMessage(val msg: String) : RegistrationEvent()
    }
}
