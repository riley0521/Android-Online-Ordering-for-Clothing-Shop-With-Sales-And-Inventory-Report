package com.teampym.onlineclothingshopapplication.presentation.admin.addeditcategory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val CATEGORY_ID = "category_id"
        private const val CATEGORY_NAME = "category_name"
        private const val FILE_NAME = "file_name"
        private const val IMAGE_URL = "image_url"
    }

    private val _categoryChannel = Channel<CategoryEvent>()
    val categoryEvent = _categoryChannel.receiveAsFlow()

    var categoryId = state.get(CATEGORY_ID) ?: ""
        set(value) {
            field = value
            state.set(CATEGORY_ID, value)
        }

    var categoryName = state.get(CATEGORY_NAME) ?: ""
        set(value) {
            field = value
            state.set(CATEGORY_NAME, value)
        }

    var fileName = state.get(FILE_NAME) ?: ""
        set(value) {
            field = value
            state.set(FILE_NAME, value)
        }

    var imageUrl = state.get(IMAGE_URL) ?: ""
        set(value) {
            field = value
            state.set(IMAGE_URL, value)
        }

    fun onSubmitClicked(category: Category, isEditMode: Boolean) = viewModelScope.launch {
        if (isEditMode && categoryId.isNotBlank()) {
            val res = async { categoryRepository.updateCategory(category) }.await()
            if (res != null) {
                resetAllUiState()
                _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Updated Category Successfully!"))
            } else {
                _categoryChannel.send(CategoryEvent.ShowErrorMessage("Updating Category Failed. Please try again later."))
            }
        } else {
            val res = async { categoryRepository.createCategory(category) }.await()
            if (res != null) {
                resetAllUiState()
                _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Added Category Successfully!"))
            } else {
                _categoryChannel.send(CategoryEvent.ShowErrorMessage("Adding Category Failed. Please try again later."))
            }
        }
    }

    private fun resetAllUiState() {
        categoryId = ""
        categoryName = ""
        fileName = ""
        imageUrl = ""
    }

    sealed class CategoryEvent {
        data class NavigateBackWithMessage(val msg: String) : CategoryEvent()
        data class ShowErrorMessage(val msg: String) : CategoryEvent()
    }
}
