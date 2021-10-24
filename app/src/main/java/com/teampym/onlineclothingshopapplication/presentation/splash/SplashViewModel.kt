package com.teampym.onlineclothingshopapplication.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.repository.AccountDeliveryInformationAndCartRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val accountRepository: AccountDeliveryInformationAndCartRepositoryImpl
) : ViewModel() {

    private val splashEventChannel = Channel<SplashEvent>()
    val splashEvent = splashEventChannel.receiveAsFlow()

    fun checkIfUserIsInDb(userId: String) = viewModelScope.launch {
        val isUserExisting = accountRepository.getUser(userId)

        if (isUserExisting) {
            splashEventChannel.send(SplashEvent.Registered)
        } else {
            splashEventChannel.send(SplashEvent.NotRegistered)
        }
    }

    sealed class SplashEvent {
        object NotRegistered: SplashEvent()
        object Registered: SplashEvent()
    }

}