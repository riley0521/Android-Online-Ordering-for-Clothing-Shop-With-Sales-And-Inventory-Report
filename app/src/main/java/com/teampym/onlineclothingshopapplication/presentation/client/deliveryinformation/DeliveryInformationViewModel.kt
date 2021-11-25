package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepository
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInformationViewModel @Inject constructor(
    private val deliveryInformationRepository: DeliveryInformationRepository,
    private val deliveryInformationDao: DeliveryInformationDao,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private var userId = ""

    private val _deliveryInformationChannel = Channel<DeliveryInfoEvent>()
    val deliveryInformationEvent = _deliveryInformationChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    private val _deliveryInformationFlow =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            userId = sessionPref.userId
            deliveryInformationRepository.getFlow(sessionPref.userId)
        }

    @ExperimentalCoroutinesApi
    val deliveryInformation = _deliveryInformationFlow.asLiveData()

    fun onDeliveryInformationDefaultChanged(
        deliveryInformation: DeliveryInformation
    ) = viewModelScope.launch {

        // modify old info to false and modify the new info to true to make it the default
        val isModified =
            deliveryInformationRepository.changeDefault(userId, deliveryInformation)
        deliveryInformationDao.insert(deliveryInformation)

        // Emit message for the view to show in snackbar
        if (isModified) {
            _deliveryInformationChannel.send(DeliveryInfoEvent.ShowMessage("Successfully changed the default delivery information."))
        } else {
            _deliveryInformationChannel.send(DeliveryInfoEvent.ShowMessage("Failed to change the default delivery information."))
        }
    }

    fun onDeleteClicked(deliveryInformation: DeliveryInformation?) = viewModelScope.launch {
        deliveryInformation?.let {
            if (deliveryInformationRepository.delete(userId, it)) {
                deliveryInformationDao.delete(it.id)
                _deliveryInformationChannel.send(
                    DeliveryInfoEvent.ShowMessage(
                        "Successfully deleted delivery information."
                    )
                )
            } else {
                _deliveryInformationChannel.send(
                    DeliveryInfoEvent.ShowMessage(
                        "Failed to delete delivery information."
                    )
                )
            }
        }
    }

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
