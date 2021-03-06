package com.teampym.onlineclothingshopapplication.presentation.client.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepository
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    var userId = ""
    var userType = ""

    init {
        viewModelScope.launch {
            val s = preferencesManager.preferencesFlow.first()
            userId = s.userId
            userType = s.userType
        }
    }

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _categoryChannel = Channel<CategoryEvent>()
    val categoryEvent = _categoryChannel.receiveAsFlow()

    fun onLoadCategories() = viewModelScope.launch {
        _categories.value = categoryRepository.getAll()
    }

    suspend fun checkIfUserIsBanned(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (userId.isNotBlank()) {
                accountRepository.checkIfUserIsBanned(userId)
            } else {
                return@withContext false
            }
        }
    }

    fun updateCategoryId(categoryId: String) = viewModelScope.launch {
        preferencesManager.updateCategoryId(categoryId)
    }

    fun onDeleteCategoryClicked(category: Category, position: Int) = viewModelScope.launch {
        val userSession = preferencesManager.preferencesFlow.first()
        val userInformation = userInformationDao.getCurrentUser(userSession.userId)

        val res = async {
            categoryRepository.delete(
                username = "${userInformation?.firstName} ${userInformation?.lastName}",
                category
            )
        }.await()
        if (res) {
            _categoryChannel.send(
                CategoryEvent.ShowSuccessMessage(
                    "Deleted category successfully!",
                    position
                )
            )
        } else {
            _categoryChannel.send(CategoryEvent.ShowErrorMessage("Deleting category failed. Please wait for a while."))
        }
    }

    sealed class CategoryEvent {
        data class ShowSuccessMessage(val msg: String, val position: Int) : CategoryEvent()
        data class ShowErrorMessage(val msg: String) : CategoryEvent()
    }
}
