package com.teampym.onlineclothingshopapplication.presentation.admin.sales_daily

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.DaySale
import com.teampym.onlineclothingshopapplication.data.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailySalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository
) : ViewModel() {

    val dailySales = MutableLiveData<List<DaySale>>()

    fun fetchDailySales(year: String, month: String) = viewModelScope.launch {
        dailySales.postValue(
            salesRepository.getDailySalesForWholeMonth(year, month)?.listOfDays
        )
    }
}
