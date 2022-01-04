package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.ADD_PROFILE_ERR
import com.teampym.onlineclothingshopapplication.ADD_PROFILE_OK
import com.teampym.onlineclothingshopapplication.EDIT_PROFILE_ERR
import com.teampym.onlineclothingshopapplication.EDIT_PROFILE_OK
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepository
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepository
import com.teampym.onlineclothingshopapplication.data.repository.NotificationTokenRepository
import com.teampym.onlineclothingshopapplication.data.repository.WishItemRepository
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val wishListRepository: WishItemRepository,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val deliveryInformationRepository: DeliveryInformationRepository,
    private val userInformationDao: UserInformationDao,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val notificationTokenDao: NotificationTokenDao,
    private val cartDao: CartDao,
    private val wishListDao: WishItemDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _profileChannel = Channel<ProfileEvent>()
    val profileEvent = _profileChannel.receiveAsFlow()

    val userSession = preferencesManager.preferencesFlow.asLiveData()

    private val _user = MutableLiveData<UserInformation>()
    val user: LiveData<UserInformation> get() = _user

    private fun onNotificationTokenInserted(userInformation: UserInformation) =
        viewModelScope.launch {
            notificationTokenRepository.getNewAndSubscribeToTopics(userInformation)?.let {
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
        resetSession()
        user.signOut()

        _profileChannel.send(ProfileEvent.SignedOut)
    }

    private fun resetSession() = viewModelScope.launch {
        preferencesManager.resetAllFields()
    }

    private fun updateUserId(userId: String) = viewModelScope.launch {
        preferencesManager.updateUserId(userId)
    }

    private fun updateUserType(userType: String) = viewModelScope.launch {
        preferencesManager.updateUserType(userType)
    }

    fun fetchUserFromRemoteDb(user: FirebaseUser) = viewModelScope.launch {
        val fetchedUser = accountRepository.get(user.uid)
        if (fetchedUser != null) {
            val fetchedWishList = async { wishListRepository.getAll(fetchedUser.userId) }
            val fetchedDeliveryInfoList =
                async { deliveryInformationRepository.getAll(fetchedUser.userId) }

            // Get Notification Based on the device
            onNotificationTokenInserted(fetchedUser)

            // Update userId in preferences
            updateUserId(fetchedUser.userId)

            // Update userType in preferences
            updateUserType(fetchedUser.userType)

            // insert data to local db
            userInformationDao.insert(fetchedUser)
            wishListDao.insertAll(fetchedWishList.await())
            deliveryInformationDao.insertAll(fetchedDeliveryInfoList.await())
        }
    }

    fun fetchUserFromLocalDb(userId: String) = viewModelScope.launch {
        val mainUser = userInformationDao.getCurrentUser(userId)
        val userWithWishList = userInformationDao.getUserWithWishList()
            .firstOrNull { it.user?.userId == userId }
        mainUser?.wishList = userWithWishList?.wishList ?: emptyList()
        _user.value = mainUser!!
    }

    fun navigateUserToRegistrationModule() = viewModelScope.launch {
        _profileChannel.send(ProfileEvent.NotRegistered)
    }

    // Send the verification again if it expired.
    fun sendVerificationAgain() = viewModelScope.launch {
        _profileChannel.send(ProfileEvent.VerificationSent)
    }

    fun onAddEditProfileResult(result: Int) = viewModelScope.launch {
        when (result) {
            ADD_PROFILE_OK, EDIT_PROFILE_OK -> {
                _profileChannel.send(ProfileEvent.ShowSuccessMessage("Profile updated successfully!"))
            }
            ADD_PROFILE_ERR, EDIT_PROFILE_ERR -> {
                _profileChannel.send(ProfileEvent.ShowErrorMessage("Updating profile failed. Please try again."))
            }
        }
    }

    // Events used to notify the UI about what's happening.
    sealed class ProfileEvent {
        object VerificationSent : ProfileEvent()
        object SignedOut : ProfileEvent()
        object NotRegistered : ProfileEvent()
        data class ShowSuccessMessage(val msg: String) : ProfileEvent()
        data class ShowErrorMessage(val msg: String) : ProfileEvent()
    }
}
