package com.teampym.onlineclothingshopapplication.presentation.registration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.ADD_PROFILE_ERR
import com.teampym.onlineclothingshopapplication.ADD_PROFILE_OK
import com.teampym.onlineclothingshopapplication.EDIT_PROFILE_ERR
import com.teampym.onlineclothingshopapplication.EDIT_PROFILE_OK
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

    private val _user = MutableLiveData<UserInformation?>()
    val user: LiveData<UserInformation?> get() = _user

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
                val isAccountUpdated = accountRepository.update(
                    userId,
                    firstName,
                    lastName,
                    birthDate,
                    photoUrl
                )

                // This should execute
                if (isAccountUpdated) {

                    // Save to local database as well
                    userInformationDao.updateBasicInfo(
                        avatarUrl = photoUrl,
                        firstName,
                        lastName,
                        birthDate,
                        userId
                    )

                    // We can finally reset the Ui State after saving it to local database
                    resetAllUiState()

                    _registrationEventChannel.send(
                        RegistrationEvent.ShowUpdatingSuccessAndNavigateBack(
                            "Updated user successfully!",
                            EDIT_PROFILE_OK
                        )
                    )
                } else {
                    // But it always goes here
                    _registrationEventChannel.send(
                        RegistrationEvent.ShowErrorMessage(
                            "Failed to update user. Please try again later."
                        )
                    )
                }
            } else {
                _registrationEventChannel.send(
                    RegistrationEvent.ShowFormErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        } else {
            if (isFormValid()) {
                val accountCreated = accountRepository.create(
                    userId,
                    firstName,
                    lastName,
                    birthDate,
                    photoUrl
                )
                if (accountCreated != null) {

                    // Save to local database
                    async { userInformationDao.insert(accountCreated) }.await()

                    // We can finally reset the Ui State after saving it to local database
                    resetAllUiState()

                    _registrationEventChannel.send(
                        RegistrationEvent.ShowAddingSuccessAndNavigateBack(
                            "Created user successfully!",
                            ADD_PROFILE_OK
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
                    RegistrationEvent.ShowFormErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        }
    }

    fun fetchUser(userId: String) = viewModelScope.launch {
        val userWithWishList = userInformationDao.getUserWithWishList()
            .firstOrNull { it.user?.userId == userId }

        val finalUser = userWithWishList?.user

        if (finalUser != null) {
            _user.value = finalUser
        }
    }

    sealed class RegistrationEvent {
        data class ShowAddingSuccessAndNavigateBack(val msg: String, val result: Int) :
            RegistrationEvent()

        data class ShowUpdatingSuccessAndNavigateBack(val msg: String, val result: Int) :
            RegistrationEvent()

        data class ShowErrorMessage(val msg: String) :
            RegistrationEvent()

        data class ShowFormErrorMessage(val msg: String) : RegistrationEvent()
    }
}
