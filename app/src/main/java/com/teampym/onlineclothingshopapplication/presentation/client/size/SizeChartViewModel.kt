package com.teampym.onlineclothingshopapplication.presentation.client.size

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.SizeChart
import com.teampym.onlineclothingshopapplication.data.repository.SizeChartRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SizeChartViewModel @Inject constructor(
    private val sizeChartRepository: SizeChartRepository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    val userSession = preferencesManager.preferencesFlow.asLiveData()

    private val _sizeChartChannel = Channel<SizeChartEvent>()
    val sizeChartEvent = _sizeChartChannel.receiveAsFlow()

    private val _sizeChartImages = MutableLiveData<List<SizeChart>>(listOf())
    val sizeChartImages: LiveData<List<SizeChart>> get() = _sizeChartImages

    fun fetchSizeChartImages() = viewModelScope.launch {
        _sizeChartImages.postValue(sizeChartRepository.getAll())
    }

    fun removeSizeChart(sizeChart: SizeChart, position: Int) = viewModelScope.launch {
        val res = sizeChartRepository.delete(sizeChart)
        if (res) {
            _sizeChartChannel.send(SizeChartEvent.ShowMessageAndNotifyAdapter("Size Chart Image deleted successfully!", position))
        } else {
            _sizeChartChannel.send(SizeChartEvent.ShowErrorMessage("Deleting size chart image failed. Please try again."))
        }
    }

    fun uploadSizeChart(imgUri: Uri) = viewModelScope.launch {
        val uploadImage = sizeChartRepository.uploadImage(imgUri)
        val createdSizeChart = sizeChartRepository.create(
            SizeChart(uploadImage.fileName, uploadImage.url)
        )
        if (createdSizeChart != null) {
            _sizeChartChannel.send(SizeChartEvent.ShowMessageAndAddItemToAdapter("Size chart image added successfully!", createdSizeChart))
        } else {
            _sizeChartChannel.send(SizeChartEvent.ShowErrorMessage("Adding size chart image failed. Please try again."))
        }
    }

    sealed class SizeChartEvent {
        data class ShowMessageAndNotifyAdapter(val msg: String, val position: Int) : SizeChartEvent()
        data class ShowMessageAndAddItemToAdapter(val msg: String, val sizeChart: SizeChart) : SizeChartEvent()
        data class ShowErrorMessage(val msg: String) : SizeChartEvent()
    }
}
