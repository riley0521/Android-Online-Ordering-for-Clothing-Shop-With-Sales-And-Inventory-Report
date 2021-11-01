package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle
) : ViewModel() {

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    private val _userInformation = MutableLiveData<UserInformation>()
    val userInformation: MutableLiveData<UserInformation> get() = _userInformation

    val verificationSpan = state.getLiveData<Long>(VERIFICATION_SPAN, 0)

    fun signOut(user: FirebaseAuth) = viewModelScope.launch {
        userInformationDao.delete(user.currentUser?.uid!!)
        user.signOut()
    }

    fun getCurrentUser(userId: String) = viewModelScope.launch {
        _userInformation.value = userInformationDao.getCurrentUser(userId)
    }

    fun checkIfUserIsRegisteredOrVerified(user: FirebaseUser) = viewModelScope.launch {

        val currentUser = accountRepository.get(user.uid)

        when {
            currentUser == null -> {
                profileEventChannel.send(ProfileEvent.NotRegistered)
            }
            user.isEmailVerified -> {
                profileEventChannel.send(ProfileEvent.Verified)
            }
            else -> {
                profileEventChannel.send(ProfileEvent.NotVerified)
            }
        }
    }

    fun sendVerificationAgain(user: FirebaseUser) = viewModelScope.launch {
        user.sendEmailVerification()
        profileEventChannel.send(ProfileEvent.VerificationSent)
    }

    companion object {
        const val VERIFICATION_SPAN = "verification_span"
    }

    sealed class ProfileEvent {
        object NotRegistered : ProfileEvent()
        object NotVerified : ProfileEvent()
        object Verified : ProfileEvent()
        object VerificationSent : ProfileEvent()
    }

}