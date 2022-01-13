package com.teampym.onlineclothingshopapplication.presentation.admin.sales

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.DaySale
import com.teampym.onlineclothingshopapplication.data.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val YEAR = "year"
        const val MONTH = "month"
        const val IS_FIRST_TIME = "is_first_time"
    }

    var year = state.getLiveData(YEAR, "2022")

    var month = state.getLiveData(MONTH, "JANUARY")

    var isFirstTime = state.get(IS_FIRST_TIME) ?: true
        set(value) {
            field = value
            state.set(IS_FIRST_TIME, value)
        }

    fun updateYear(y: String) {
        year.postValue(y)
    }

    fun updateMonth(m: String) {
        month.postValue(m)
    }

    val salesForSelectedMonthAndYear = combine(
        year.asFlow(),
        month.asFlow()
    ) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, _) ->
        flowOf(salesRepository.getSalesForWholeYear(year))
    }
}
