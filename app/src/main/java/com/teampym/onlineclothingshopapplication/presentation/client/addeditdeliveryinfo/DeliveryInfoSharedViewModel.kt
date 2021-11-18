package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.models.Selector
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInfoSharedViewModel @Inject constructor(
    private val regionDao: RegionDao,
    private val provinceDao: ProvinceDao,
    private val cityDao: CityDao,
    private val deliveryInformationRepository: DeliveryInformationRepositoryImpl,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    private val _userId = MutableLiveData("")
    val userId: LiveData<String> get() = _userId

    private val _deliveryInformationChannel = Channel<AddEditDeliveryInformationEvent>()
    val event = _deliveryInformationChannel.receiveAsFlow()

    private val _userDeliveryInfoList = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        _userId.value = sessionPref.userId
        deliveryInformationDao.getAll(sessionPref.userId)
    }

    val userDeliveryInfoList = _userDeliveryInfoList.asLiveData()

    private val _regionId = MutableLiveData(0L)
    val regionId: LiveData<Long> get() = _regionId

    private val _provinceId = MutableLiveData(0L)
    val provinceId: LiveData<Long> get() = _provinceId

    private val _selectedRegion = MutableLiveData<Region>()
    val selectedRegion: LiveData<Region> get() = _selectedRegion

    private val _selectedProvince = MutableLiveData<Province>()
    val selectedProvince: LiveData<Province> get() = _selectedProvince

    private val _selectedCity = MutableLiveData<City>()
    val selectedCity: LiveData<City> get() = _selectedCity

    val regions = regionDao.getAll().asLiveData()

    fun getAllProvince(regionId: Long): LiveData<List<Province>> {
        return provinceDao.getAll(regionId).asLiveData()
    }

    fun getAllCity(provinceId: Long): LiveData<List<City>> {
        return cityDao.getAll(provinceId).asLiveData()
    }

    fun onSelectedRegion(selector: Selector) = viewModelScope.launch {
        _selectedRegion.value = Region(id = selector.id, name = selector.name)
        _regionId.value = selector.id
    }

    fun onSelectedProvince(selector: Selector) = viewModelScope.launch {
        _selectedProvince.value = Province(id = selector.id, regionId = selector.parentId, name = selector.name)
        _provinceId.value = selector.id
    }

    fun onSelectedCity(selector: Selector) = viewModelScope.launch {
        _selectedCity.value = City(id = selector.id, provinceId = selector.parentId, name = selector.name)
    }

    fun onDeleteAddressClicked(deliveryInfo: DeliveryInformation?) = appScope.launch {
        _userId.value?.let { userId ->
            if (userId.isNotBlank()) {
                deliveryInfo?.let {
                    if (deliveryInformationRepository.delete(userId, it)) {
                        deliveryInformationDao.delete(it.id)
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                DELETE_DELIVERY_INFO_RESULT_OK
                            )
                        )
                    } else {
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                DELETE_DELIVERY_INFO_RESULT_ERR
                            )
                        )
                    }
                }
            } else {
                _deliveryInformationChannel.send(AddEditDeliveryInformationEvent.NavigateBackWithResult(DELETE_DELIVERY_INFO_RESULT_ERR))
            }
        }
    }

    fun onSubmitClicked(deliveryInfo: DeliveryInformation, isEditing: Boolean) = appScope.launch {
        _userId.value?.let {
            if (it.isNotBlank()) {

                if (isEditing) {
                    if (deliveryInformationRepository.upsert(it, deliveryInfo)) {
                        deliveryInformationDao.insert(deliveryInfo)
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                EDIT_DELIVERY_INFO_RESULT_OK
                            )
                        )
                    } else {
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                EDIT_DELIVERY_INFO_RESULT_ERR
                            )
                        )
                    }
                } else {
                    if (deliveryInformationRepository.upsert(it, deliveryInfo)) {
                        deliveryInformationDao.insert(deliveryInfo)
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                ADD_DELIVERY_INFO_RESULT_OK
                            )
                        )
                    } else {
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                ADD_DELIVERY_INFO_RESULT_ERR
                            )
                        )
                    }
                }
            }
        }
    }

    sealed class AddEditDeliveryInformationEvent {
        data class NavigateBackWithResult(val result: Int) : AddEditDeliveryInformationEvent()
    }
}
