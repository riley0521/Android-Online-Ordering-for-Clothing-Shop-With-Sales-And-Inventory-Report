package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.VERIFICATION_SPAN
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.NotificationTokenRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
    private val notificationTokenRepository: NotificationTokenRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val notificationTokenDao: NotificationTokenDao,
    private val cartDao: CartDao,
    private val wishListDao: WishItemDao,
    state: SavedStateHandle,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val profileEventChannel = Channel<ProfileVerificationEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    val verificationSpan = state.getLiveData<Long>(VERIFICATION_SPAN, 0)

    val userSession = preferencesManager.preferencesFlow.asLiveData()

    private val _user = MutableLiveData<UserInformation>()
    val user: LiveData<UserInformation> get() = _user

    private val _isRegistered = MutableLiveData(true)
    val isRegistered: LiveData<Boolean> get() = _isRegistered

    fun onNotificationTokenInserted(userId: String, userType: String, token: String) =
        viewModelScope.launch {
            notificationTokenRepository.insert(userId, userType, token)?.let {
                notificationTokenDao.insert(it)
            }
        }

    // Remove the FirebaseAuth cache and in the room db.
    fun signOut(user: FirebaseAuth) = viewModelScope.launch {
        user.let { u ->
            u.uid?.let {
                userInformationDao.deleteAll(it)
                deliveryInformationDao.deleteAll(it)
                notificationTokenDao.deleteAll(it)
                cartDao.deleteAll(it)
                wishListDao.deleteAll(it)
            }
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

    fun fetchNotificationTokensAndWishList(userId: String) = viewModelScope.launch {
        val mainUser = userInformationDao.getCurrentUser(userId)
        val userWithNotificationTokens = userInformationDao.getUserWithNotificationTokens()
            .firstOrNull { it.user.userId == userId }
        val userWithWishList = userInformationDao.getUserWithWishList()
            .firstOrNull { it.user.userId == userId }
        _user.value = mainUser?.copy(
            notificationTokenList = userWithNotificationTokens?.notificationTokens ?: emptyList(),
            wishList = userWithWishList?.wishList ?: emptyList()
        )
    }

    fun checkIfUserIsRegistered(user: FirebaseUser) = viewModelScope.launch {
        val currentUser = accountRepository.get(user.uid)

        currentUser?.let {
            updateUserId(it.userId)
            if (currentUser.firstName.isBlank()) {
                _isRegistered.value = false
            }
        }
    }

    // Check if the user is registered and/or verified then save
    // the userId in UI State.
    fun checkIfUserIsEmailVerified(user: FirebaseUser) = viewModelScope.launch {
        if (!user.isEmailVerified) {
            updateUserId(user.uid)
            profileEventChannel.send(ProfileVerificationEvent.NotVerified)
        } else {
            updateUserId(user.uid)
            profileEventChannel.send(ProfileVerificationEvent.Verified)
        }
    }

    // Send the verification again if it expired.
    fun sendVerificationAgain(user: FirebaseUser) = viewModelScope.launch {
        user.sendEmailVerification()
        profileEventChannel.send(ProfileVerificationEvent.VerificationSent)
    }

    // Events used to notify the UI about what's happening.
    sealed class ProfileVerificationEvent {
        object NotVerified : ProfileVerificationEvent()
        object Verified : ProfileVerificationEvent()
        object VerificationSent : ProfileVerificationEvent()
    }
}
