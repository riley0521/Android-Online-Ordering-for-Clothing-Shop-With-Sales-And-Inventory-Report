package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.teampym.onlineclothingshopapplication.data.models.Utils
import com.teampym.onlineclothingshopapplication.data.repository.AccountAndDeliveryInformationImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val accountRepository: AccountAndDeliveryInformationImpl,
    private val state: SavedStateHandle
) : ViewModel() {

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    val verificationSpan = state.getLiveData<Long>(VERIFICATION_SPAN, 0)

    fun signOut(user: FirebaseAuth) = viewModelScope.launch {
        user.signOut()
        Utils.currentUser = null
    }

    fun checkIfUserIsVerified(user: FirebaseUser) = viewModelScope.launch {

        if (user.isEmailVerified) {
            accountRepository.getUser(user.uid)
        } else {
            profileEventChannel.send(ProfileEvent.NotVerified)
            accountRepository.getUser(user.uid)
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
        object VerificationSent : ProfileEvent()
    }

}