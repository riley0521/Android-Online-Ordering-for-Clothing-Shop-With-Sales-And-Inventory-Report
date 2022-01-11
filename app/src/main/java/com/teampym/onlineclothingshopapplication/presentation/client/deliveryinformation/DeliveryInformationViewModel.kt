package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInformationViewModel @Inject constructor(
    private val deliveryInformationRepository: DeliveryInformationRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val _deliveryInformationChannel = Channel<DeliveryInfoEvent>()
    val deliveryInformationEvent = _deliveryInformationChannel.receiveAsFlow()

    private val _deliveryInformationFlow =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            deliveryInformationRepository.getFlow(sessionPref.userId)
        }

    val deliveryInformation = _deliveryInformationFlow.asLiveData()

    fun onAddEditOrDeleteResult(result: Int) = viewModelScope.launch {
        when (result) {
            ADD_DELIVERY_INFO_RESULT_OK -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Successfully added delivery information."
                )
            )
            EDIT_DELIVERY_INFO_RESULT_OK -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Successfully updated delivery information."
                )
            )
            DELETE_DELIVERY_INFO_RESULT_OK -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Successfully deleted delivery information."
                )
            )
            ADD_DELIVERY_INFO_RESULT_ERR -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Failed to add delivery information."
                )
            )
            EDIT_DELIVERY_INFO_RESULT_ERR -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Failed to update delivery information."
                )
            )
            DELETE_DELIVERY_INFO_RESULT_ERR -> _deliveryInformationChannel.send(
                DeliveryInfoEvent.ShowMessage(
                    "Failed to delete delivery information."
                )
            )
        }
    }

    sealed class DeliveryInfoEvent {
        data class ShowMessage(val msg: String) : DeliveryInfoEvent()
    }
}
