package com.teampym.onlineclothingshopapplication.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val accountRepository: AccountRepositoryImpl
) : ViewModel() {

    private val splashEventChannel = Channel<SplashEvent>()
    val splashEvent = splashEventChannel.receiveAsFlow()

    fun checkIfUserIsInDb(userId: String) = viewModelScope.launch {
        when (accountRepository.get(userId)) {
            is Resource.Error -> splashEventChannel.send(SplashEvent.NotRegistered)
            is Resource.Success -> splashEventChannel.send(SplashEvent.Registered)
        }
    }

    sealed class SplashEvent {
        object NotRegistered : SplashEvent()
        object Registered : SplashEvent()
    }
}
