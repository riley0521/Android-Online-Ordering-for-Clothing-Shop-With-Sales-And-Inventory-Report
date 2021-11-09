package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInformationViewModel @Inject constructor(
    private val deliveryInformationRepository: DeliveryInformationRepositoryImpl,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var userId = ""

    private val deliveryInformationFlow =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            userId = sessionPref.userId
            deliveryInformationRepository.getFlow(sessionPref.userId)
        }

    val deliveryInformation = deliveryInformationFlow.asLiveData()

    fun onDeliveryInformationDefaultChanged(
        defaultDeliveryInfo: DeliveryInformation?,
        deliveryInfo: DeliveryInformation
    ) = viewModelScope.launch {
        deliveryInformationRepository.changeDefault(userId, defaultDeliveryInfo, deliveryInfo)
        if (defaultDeliveryInfo != null) {
            deliveryInformationDao.update(defaultDeliveryInfo)
        }
        deliveryInformationDao.update(deliveryInfo)
    }
}
