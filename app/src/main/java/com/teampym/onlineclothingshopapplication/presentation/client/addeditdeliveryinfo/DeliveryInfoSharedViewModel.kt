package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.ADD_DELIVERY_INFO_RESULT_OK
import com.teampym.onlineclothingshopapplication.DELETE_DELIVERY_INFO_RESULT_OK
import com.teampym.onlineclothingshopapplication.EDIT_DELIVERY_INFO_RESULT_OK
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Selector
import com.teampym.onlineclothingshopapplication.data.repository.DeliveryInformationRepository
import com.teampym.onlineclothingshopapplication.data.room.City
import com.teampym.onlineclothingshopapplication.data.room.CityDao
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Province
import com.teampym.onlineclothingshopapplication.data.room.ProvinceDao
import com.teampym.onlineclothingshopapplication.data.room.Region
import com.teampym.onlineclothingshopapplication.data.room.RegionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "DeliveryViewModel"

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

    fun onDeleteAddressClicked(deliveryInfo: DeliveryInformation?) = viewModelScope.launch {
        _userId.value?.let { userId ->
            if (userId.isNotBlank()) {
                if (deliveryInfo != null) {
                    val res = deliveryInformationRepository.delete(userId, deliveryInfo)
                    if (res) {
                        deliveryInformationDao.delete(deliveryInfo.id)

                        resetAllUiState()
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                DELETE_DELIVERY_INFO_RESULT_OK
                            )
                        )
                    } else {
                        _deliveryInformationChannel.send(
                            AddEditDeliveryInformationEvent.ShowErrorMessage(
                                "Deleting delivery information failed. Please try again."
                            )
                        )
                    }
                }
            } else {
                _deliveryInformationChannel.send(
                    AddEditDeliveryInformationEvent.ShowErrorMessage(
                        "User ID not found."
                    )
                )
            }
        }
    }

    private fun isFormValid(del: DeliveryInformation): Boolean {
        return fullName.isNotBlank() &&
            phoneNumber.isNotBlank() &&
            postalCode.isNotBlank() &&
            streetNumber.isNotBlank() &&
            del.region.isNotBlank() &&
            del.province.isNotBlank() &&
            del.city.isNotBlank()
    }

    fun onSubmitClicked(deliveryInfo: DeliveryInformation, isEditing: Boolean) =
        viewModelScope.launch {
            _userId.value?.let {
                if (it.isNotBlank()) {
                    if (isEditing) {
                        if (isFormValid(deliveryInfo)) {
                            val res = deliveryInformationRepository.update(it, deliveryInfo)
                            if (res) {
                                insertToLocalDbAndChangeDefault(deliveryInfo)

                                // Send signal to other fragment
                                _deliveryInformationChannel.send(
                                    AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                        EDIT_DELIVERY_INFO_RESULT_OK
                                    )
                                )
                            } else {
                                _deliveryInformationChannel.send(
                                    AddEditDeliveryInformationEvent.ShowErrorMessage(
                                        "Updating delivery information failed. Please try again."
                                    )
                                )
                            }
                        } else {
                            _deliveryInformationChannel.send(
                                AddEditDeliveryInformationEvent.ShowErrorMessage(
                                    "Please fill the form."
                                )
                            )
                        }
                    } else {
                        if (isFormValid(deliveryInfo)) {
                            val res = deliveryInformationRepository.create(it, deliveryInfo)
                            if (res != null) {
                                insertToLocalDbAndChangeDefault(res)

                                // Send signal to other fragment
                                _deliveryInformationChannel.send(
                                    AddEditDeliveryInformationEvent.NavigateBackWithResult(
                                        ADD_DELIVERY_INFO_RESULT_OK
                                    )
                                )
                            } else {
                                _deliveryInformationChannel.send(
                                    AddEditDeliveryInformationEvent.ShowErrorMessage(
                                        "Creating delivery information failed. Please try again."
                                    )
                                )
                            }
                        } else {
                            _deliveryInformationChannel.send(
                                AddEditDeliveryInformationEvent.ShowErrorMessage(
                                    "Please fill the form."
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

    private suspend fun insertToLocalDbAndChangeDefault(deliveryInfo: DeliveryInformation) {
        withContext(Dispatchers.IO) {
            deliveryInformationDao.insert(deliveryInfo)
            try {
                if (deliveryInfo.isPrimary) {
                    deliveryInformationRepository.changeDefault(
                        deliveryInfo.userId,
                        deliveryInfo
                    )
                } else {
                }
            } catch (ex: Exception) {
                ex.message?.let { Log.d(TAG, it) }
            }
        }
    }

    sealed class AddEditDeliveryInformationEvent {
        data class NavigateBackWithResult(val result: Int) : AddEditDeliveryInformationEvent()
        data class ShowErrorMessage(val msg: String) : AddEditDeliveryInformationEvent()
    }
}
