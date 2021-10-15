package com.teampym.onlineclothingshopapplication.presentation.client.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.models.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val state: SavedStateHandle
) : ViewModel() {

    private val profileEventChannel = Channel<ProfileEvent>()
    val profileEvent = profileEventChannel.receiveAsFlow()

    val verificationSpan = state.getLiveData<Long>(VERIFICATION_SPAN, 0)

    fun signOut(user: FirebaseAuth) = viewModelScope.launch {
        user.signOut()
    }

    private fun checkIfUserIsInDb(user: FirebaseUser) = viewModelScope.launch {
        val isExisting = db.collection("Users")
            .whereEqualTo("userId", user.uid)
            .limit(1)
            .get()
            .await()

        if (isExisting.size() == 1) {

            val selectedUser = isExisting.documents[0]

            Utils.currentUser = UserInformation(
                selectedUser.id,
                selectedUser.getString("userId")!!,
                selectedUser.getString("firstName")!!,
                selectedUser.getString("lastName")!!,
                selectedUser.getString("avatarUrl")!!,
                selectedUser.getString("contactInformation")!!,
                selectedUser.getString("userType")!!
            )

        } else {
            profileEventChannel.send(ProfileEvent.NotRegistered)
        }
    }

    fun checkIfUserIsVerified(user: FirebaseUser) = viewModelScope.launch {

        if (user.isEmailVerified) {
            checkIfUserIsInDb(user)
        } else {
            profileEventChannel.send(ProfileEvent.NotVerified)
            checkIfUserIsInDb(user)
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