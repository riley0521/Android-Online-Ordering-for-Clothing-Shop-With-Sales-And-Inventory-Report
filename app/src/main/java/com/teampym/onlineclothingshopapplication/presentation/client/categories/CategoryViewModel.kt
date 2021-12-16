package com.teampym.onlineclothingshopapplication.presentation.client.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _categoryChannel = Channel<CategoryEvent>()
    val categoryEvent = _categoryChannel.receiveAsFlow()

    val userFlow = preferencesManager.preferencesFlow.flatMapLatest { sessionPref ->
        userInformationDao.get(sessionPref.userId)
    }

    fun updateUserId(userId: String) = viewModelScope.launch {
        preferencesManager.updateUserId(userId)
    }

    fun loadCategories() = viewModelScope.launch {
        _categories.value = categoryRepository.getAll()
    }

    fun onDeleteCategoryClicked(category: Category, position: Int) = viewModelScope.launch {
        val res = async { categoryRepository.delete(category.id) }.await()
        if (res) {
            _categoryChannel.send(CategoryEvent.ShowSuccessMessage("Deleted category successfully!", position))
        } else {
            _categoryChannel.send(CategoryEvent.ShowErrorMessage("Deleting category failed. Please wait for a while."))
        }
    }

    sealed class CategoryEvent {
        data class ShowSuccessMessage(val msg: String, val position: Int) : CategoryEvent()
        data class ShowErrorMessage(val msg: String) : CategoryEvent()
    }
}
