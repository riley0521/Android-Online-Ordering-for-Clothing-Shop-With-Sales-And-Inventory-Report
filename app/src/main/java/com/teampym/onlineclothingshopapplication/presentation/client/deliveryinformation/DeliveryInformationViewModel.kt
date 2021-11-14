package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInformationViewModel @Inject constructor(
    private val deliveryInformationRepository: DeliveryInformationRepositoryImpl,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private var userId = ""

    private val deliveryInformationChannel = Channel<DeliveryInfoEvent>()
    val deliveryInfoEvent = deliveryInformationChannel.receiveAsFlow()

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

        // modify old info to false and modify the new info to true to make it the default
        val isModified = deliveryInformationRepository.changeDefault(userId, defaultDeliveryInfo, deliveryInfo)
        if (defaultDeliveryInfo != null) {
            deliveryInformationDao.update(defaultDeliveryInfo)
        }
        deliveryInformationDao.update(deliveryInfo)

        // Emit message for the view to show in snackbar
        if (isModified) {
            deliveryInformationChannel.send(DeliveryInfoEvent.ShowMessage("Successfully changed the default delivery information."))
        } else {
            deliveryInformationChannel.send(DeliveryInfoEvent.ShowMessage("Failed to change the default delivery information."))
        }
    }

    sealed class DeliveryInfoEvent {
        data class ShowMessage(val msg: String) : DeliveryInfoEvent()
    }
}
