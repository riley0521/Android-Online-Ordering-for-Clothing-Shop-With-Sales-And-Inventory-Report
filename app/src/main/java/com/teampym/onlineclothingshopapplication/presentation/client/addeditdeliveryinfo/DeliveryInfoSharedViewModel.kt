package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import androidx.lifecycle.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.db.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.models.Selector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryInfoSharedViewModel @Inject constructor(
    private val regionDao: RegionDao,
    private val provinceDao: ProvinceDao,
    private val cityDao: CityDao,
    private val deliveryInformationDao: DeliveryInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _userDeliveryInfoList = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
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
}
