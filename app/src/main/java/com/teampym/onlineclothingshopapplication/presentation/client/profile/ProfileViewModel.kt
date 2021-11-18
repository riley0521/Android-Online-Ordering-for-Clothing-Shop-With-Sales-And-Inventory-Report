package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.VERIFICATION_SPAN
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val notificationTokenDao: NotificationTokenDao,
    private val state: SavedStateHandle,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    val verificationSpan = state.getLiveData<Long>(VERIFICATION_SPAN, 0)

    private val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.getUserFlow(sessionPref.userId)
    }

    val user = userFlow.asLiveData()

    // Remove the FirebaseAuth cache and in the room db.
    fun signOut(user: FirebaseAuth) = viewModelScope.launch {
        user.let {
            userInformationDao.deleteAll(it.uid!!)
            deliveryInformationDao.deleteAll(it.uid!!)
            notificationTokenDao.deleteAll(it.uid!!)
        }

        // I think I need to use dataStore to replace Sorting Mechanism
        // And this user Id to store session.
        removeUserIdFromSession()
        user.signOut()
    }

    private fun removeUserIdFromSession() = viewModelScope.launch {
        preferencesManager.updateUserId("")
    }

    private fun updateUserId(userId: String) = viewModelScope.launch {
        preferencesManager.updateUserId(userId)
    }

    // Check if the user is registered and/or verified then save
    // the userId in UI State.
    fun checkIfUserIsRegisteredOrVerified(user: FirebaseUser) = viewModelScope.launch {

        val currentUser = accountRepository.get(user.uid)

        currentUser?.let {
            if (currentUser.firstName.isBlank()) {
                profileEventChannel.send(ProfileEvent.NotRegistered)
            }
        }

        user.phoneNumber?.let {
            if (it.isNotBlank())
                return@launch
        }

        when {
            user.isEmailVerified.not() -> {
                updateUserId(user.uid)
                profileEventChannel.send(ProfileEvent.NotVerified)
            }
            user.isEmailVerified -> {
                updateUserId(user.uid)
                profileEventChannel.send(ProfileEvent.Verified)
            }
        }
    }

    // Send the verification again if it expired.
    fun sendVerificationAgain(user: FirebaseUser) = viewModelScope.launch {
        user.sendEmailVerification()
        profileEventChannel.send(ProfileEvent.VerificationSent)
    }

    // Events used to notify the UI about what's happening.
    sealed class ProfileEvent {
        object NotRegistered : ProfileEvent()
        object NotVerified : ProfileEvent()
        object Verified : ProfileEvent()
        object VerificationSent : ProfileEvent()
    }
}
