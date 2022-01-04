package com.teampym.onlineclothingshopapplication.presentation.client.others

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CancelReasonViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val userInformationDao: UserInformationDao,
    private val state: SavedStateHandle,
    private val preferencesManager: PreferencesManager,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    companion object {
        private const val CANCEL_REASON = "cancel_reason"
    }

    private var userId = ""
    private var _userType = MutableLiveData("")

    private var _isSuccessful = MutableLiveData<Boolean>(false)
    val isSuccessful: LiveData<Boolean> get() = _isSuccessful

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userId = sessionPref.userId
        userInformationDao.get(sessionPref.userId)
    }

    var cancelReason = state.get(CANCEL_REASON) ?: ""
        set(value) {
            field = value
            state.set(CANCEL_REASON, value)
        }

    fun updateUserType(userType: String) = viewModelScope.launch {
        _userType.value = userType
    }

    fun cancelOrderAdmin(order: Order, cancelReason: String) = appScope.launch {
        if (_userType.value != null) {
            val userInformation = userFlow.first()

            _isSuccessful.value = orderRepository.updateOrderStatus(
                username = "${userInformation?.firstName} ${userInformation?.lastName}",
                userId,
                _userType.value!!,
                order.id,
                Status.CANCELED.name,
                cancelReason,
                null
            )
        }
    }
}
