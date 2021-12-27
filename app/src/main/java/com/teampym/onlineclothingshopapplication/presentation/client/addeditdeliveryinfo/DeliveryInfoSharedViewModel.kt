package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Selector
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepository
import com.teampym.onlineclothingshopapplication.data.room.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInfoSharedViewModel @Inject constructor(
    regionDao: RegionDao,
    private val provinceDao: ProvinceDao,
    private val cityDao: CityDao,
    private val deliveryInformationRepository: DeliveryInformationRepository,
    private val deliveryInformationDao: DeliveryInformationDao,
    preferencesManager: PreferencesManager,
    private val state: SavedStateHandle,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    companion object {
        private const val FULL_NAME = "full_name"
        private const val PHONE_NUMBER = "phone_number"
        private const val REGION = "region"
        private const val PROVINCE = "province"
        private const val CITY = "city"
        private const val POSTAL_CODE = "postal_code"
        private const val STREET_NUMBER = "street_number"
    }

    private val _userId = MutableLiveData("")
    val userId: LiveData<String> get() = _userId

    private val _deliveryInformationChannel = Channel<AddEditDeliveryInformationEvent>()
    val event = _deliveryInformationChannel.receiveAsFlow()

    @ExperimentalCoroutinesApi
    private val _userDeliveryInfoList =
        preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
            _userId.value = sessionPref.userId
            deliveryInformationDao.getAll(sessionPref.userId)
        }

    @ExperimentalCoroutinesApi
    val userDeliveryInfoList = _userDeliveryInfoList.asLiveData()

    var fullName = state.get<String>(FULL_NAME) ?: ""
        set(value) {
            field = value
            state.set(FULL_NAME, value)
        }

    var phoneNumber = state.get<String>(PHONE_NUMBER) ?: ""
        set(value) {
            field = value
            state.set(PHONE_NUMBER, value)
        }

    var postalCode = state.get<String>(POSTAL_CODE) ?: ""
        set(value) {
            field = value
            state.set(POSTAL_CODE, value)
        }

    var streetNumber = state.get<String>(STREET_NUMBER) ?: ""
        set(value) {
            field = value
            state.set(STREET_NUMBER, value)
        }

    private val _regionId = MutableLiveData(0L)
    val regionId: LiveData<Long> get() = _regionId

    private val _provinceId = MutableLiveData(0L)
    val provinceId: LiveData<Long> get() = _provinceId

    val selectedRegion: MutableLiveData<Region> = state.getLiveData(REGION, Region())

    val selectedProvince: MutableLiveData<Province> = state.getLiveData(PROVINCE, Province())

    val selectedCity: MutableLiveData<City> = state.getLiveData(CITY, City())

    private fun updateRegion(region: Region) {
        selectedRegion.value = region
        state.set(REGION, region)
    }

    private fun updateProvince(province: Province) {
        selectedProvince.value = province
        state.set(PROVINCE, province)
    }

    private fun updateCity(city: City) {
        selectedCity.value = city
        state.set(CITY, city)
    }

    val regions = regionDao.getAll().asLiveData()

    fun getAllProvince(regionId: Long): LiveData<List<Province>> {
        return provinceDao.getAll(regionId).asLiveData()
    }

    fun getAllCity(provinceId: Long): LiveData<List<City>> {
        return cityDao.getAll(provinceId).asLiveData()
    }

    fun onSelectedRegion(selector: Selector) = viewModelScope.launch {
        updateRegion(Region(id = selector.id, name = selector.name))
        _regionId.value = selector.id
    }

    fun onSelectedProvince(selector: Selector) = viewModelScope.launch {
        updateProvince(
            Province(
                id = selector.id,
                regionId = selector.parentId,
                name = selector.name
            )
        )
        _provinceId.value = selector.id
    }

    fun onSelectedCity(selector: Selector) = viewModelScope.launch {
        updateCity(City(id = selector.id, provinceId = selector.parentId, name = selector.name))
    }

    fun onDeleteAddressClicked(deliveryInfo: DeliveryInformation?) = appScope.launch {
        _userId.value?.let { userId ->
            if (userId.isNotBlank()) {
                if (deliveryInfo != null) {
                    val res = async {
                        deliveryInformationRepository.delete(userId, deliveryInfo)
                    }.await()
                    if (res) {
                        async { deliveryInformationDao.delete(deliveryInfo.id) }.await()
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
                _deliveryInformationChannel.send(
                    AddEditDeliveryInformationEvent.NavigateBackWithResult(
                        DELETE_DELIVERY_INFO_RESULT_ERR
                    )
                )
            }
        }
    }

    fun onSubmitClicked(deliveryInfo: DeliveryInformation, isEditing: Boolean) = appScope.launch {
        _userId.value?.let {
            if (it.isNotBlank()) {

                if (isEditing) {
                    val res =
                        async { deliveryInformationRepository.update(it, deliveryInfo) }.await()
                    if (res) {
                        async { insertToLocalDbAndChangeDefault(deliveryInfo) }.await()

                        // Send signal to other fragment
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
                    val res =
                        async { deliveryInformationRepository.create(it, deliveryInfo) }.await()
                    if (res != null) {
                        async { insertToLocalDbAndChangeDefault(deliveryInfo) }.await()

                        // Send signal to other fragment
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

    private fun resetAllUiState() {
        fullName = ""
        phoneNumber = ""
        postalCode = ""
        streetNumber = ""
        updateRegion(Region())
        updateProvince(Province())
        updateCity(City())
    }

    override fun onCleared() {
        resetAllUiState()
        super.onCleared()
    }

    private fun insertToLocalDbAndChangeDefault(deliveryInfo: DeliveryInformation) =
        viewModelScope.launch {
            async { deliveryInformationDao.insert(deliveryInfo) }.await()
            if (deliveryInfo.isPrimary) {
                async {
                    deliveryInformationRepository.changeDefault(
                        deliveryInfo.userId,
                        deliveryInfo
                    )
                }.await()
            }
        }

    sealed class AddEditDeliveryInformationEvent {
        data class NavigateBackWithResult(val result: Int) : AddEditDeliveryInformationEvent()
    }
}
