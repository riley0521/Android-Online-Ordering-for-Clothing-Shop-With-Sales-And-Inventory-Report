package com.teampym.onlineclothingshopapplication.presentation.client.orders

import androidx.lifecycle.ViewModel
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    preferencesManager: PreferencesManager,
    private val userInformationDao: UserInformationDao
) : ViewModel() {

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }
}
