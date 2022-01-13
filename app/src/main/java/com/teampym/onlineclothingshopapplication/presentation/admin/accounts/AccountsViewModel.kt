package com.teampym.onlineclothingshopapplication.presentation.admin.accounts

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.USERS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    db: FirebaseFirestore,
    private val accountRepository: AccountRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _accountsChannel = Channel<AccountsEvent>()
    val accountsEvent = _accountsChannel.receiveAsFlow()

    val searchQuery = MutableLiveData("")

    val accounts = searchQuery.asFlow().flatMapLatest { search ->

        val query = if (search.isEmpty()) {
            db.collection(USERS_COLLECTION)
                .whereEqualTo("userType", UserType.CUSTOMER.name)
                .limit(30)
        } else {
            db.collection(USERS_COLLECTION)
                .whereEqualTo(FieldPath.documentId(), search)
                .limit(30)
        }

        accountRepository.getAll(query).flow.cachedIn(viewModelScope)
    }

    var userId = ""
    var username = ""

    init {
        viewModelScope.launch {
            userId = preferencesManager.preferencesFlow.first().userId
            val userInfo = userInformationDao.getCurrentUser(userId)
            username = "${userInfo?.firstName} ${userInfo?.lastName}"
        }
    }

    fun banUser(user: UserInformation) = viewModelScope.launch {
        val res = accountRepository.banUser(username, user)
        if (res) {
            _accountsChannel.send(AccountsEvent.ShowSuccessMessage("Account is banned successfully!"))
        } else {
            _accountsChannel.send(AccountsEvent.ShowErrorMessage("Banning account failed. Please try again."))
        }
    }

    fun unBanUser(user: UserInformation) = viewModelScope.launch {
        val res = accountRepository.unBanUser(username, user)
        if (res) {
            _accountsChannel.send(AccountsEvent.ShowSuccessMessage("Account is unbanned successfully!"))
        } else {
            _accountsChannel.send(AccountsEvent.ShowErrorMessage("Unbanning account failed. Please try again."))
        }
    }

    sealed class AccountsEvent {
        data class ShowSuccessMessage(val msg: String) : AccountsEvent()
        data class ShowErrorMessage(val msg: String) : AccountsEvent()
    }
}
