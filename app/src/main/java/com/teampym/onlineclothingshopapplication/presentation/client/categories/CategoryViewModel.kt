package com.teampym.onlineclothingshopapplication.presentation.client.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _categoryChannel = Channel<CategoryEvent>()
    val categoryEvent = _categoryChannel.receiveAsFlow()

    fun loadCategories() = viewModelScope.launch {
        _categories.value = categoryRepository.getAll()
    }

    fun getUserSession(): LiveData<SessionPreferences> {
        return preferencesManager.preferencesFlow.asLiveData()
    }

    fun updateCategoryId(categoryId: String) = viewModelScope.launch {
        preferencesManager.updateCategoryId(categoryId)
    }

    fun onDeleteCategoryClicked(category: Category, position: Int) = viewModelScope.launch {
        val res = async { categoryRepository.delete(category.id) }.await()
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
