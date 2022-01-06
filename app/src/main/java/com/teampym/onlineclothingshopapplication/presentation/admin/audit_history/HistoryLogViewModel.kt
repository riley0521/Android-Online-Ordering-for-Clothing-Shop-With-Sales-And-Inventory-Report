package com.teampym.onlineclothingshopapplication.presentation.admin.audit_history

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.models.AuditTrail
import com.teampym.onlineclothingshopapplication.data.repository.AuditTrailRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.AUDIT_TRAILS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryLogViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val repository: AuditTrailRepository,
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val FILTER_TYPE = "filter_type"
    }

    var filterType = state.get(FILTER_TYPE) ?: AuditType.CATEGORY.name
        private set(value) {
            field = value
            state.set(FILTER_TYPE, value)
        }

    private val _historyLogs = preferencesManager.preferencesFlow.flatMapLatest { session ->
        val query = db.collection(AUDIT_TRAILS_COLLECTION)
            .whereEqualTo("type", session.filterLogType)
            .orderBy("dateOfLog", Query.Direction.DESCENDING)
            .limit(30)

        filterType = session.filterLogType.name

        repository.getSome(query).flow.cachedIn(viewModelScope)
    }

    val historyLogs: LiveData<PagingData<AuditTrail>> get() = _historyLogs.asLiveData()

    fun updateFilterLogType(auditType: AuditType) = viewModelScope.launch {
        preferencesManager.updateFilterLogType(auditType)
    }
}
