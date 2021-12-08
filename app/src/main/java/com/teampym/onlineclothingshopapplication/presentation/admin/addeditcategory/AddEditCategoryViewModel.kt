package com.teampym.onlineclothingshopapplication.presentation.admin.addeditcategory

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import com.teampym.onlineclothingshopapplication.data.util.Utils
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

    val fileName: MutableLiveData<String> = state.getLiveData(FILE_NAME, "")

    val imageUrl: MutableLiveData<String> = state.getLiveData(IMAGE_URL, "")

    suspend fun updateFileName(name: String) {
        fileName.postValue(name)
    }

    suspend fun updateImageUrl(url: String) {
        imageUrl.postValue(url)
    }

    fun onSubmitClicked(category: Category?, isEditMode: Boolean) = viewModelScope.launch {
        if (isEditMode && category != null && category.id.isNotBlank()) {
            if (isFormValid()) {
                val res = async { categoryRepository.update(category) }.await()
                if (res != null) {
                    resetAllUiState()
                    _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Updated Category Successfully!"))
                } else {
                    _categoryChannel.send(CategoryEvent.ShowErrorMessage("Updating Category Failed. Please try again later."))
                }
            }
        } else {
            if (isFormValid()) {
                val newCategory = Category(
                    categoryName,
                    fileName.value!!,
                    imageUrl.value!!,
                    dateAdded = Utils.getTimeInMillisUTC()
                )
                val res = async { categoryRepository.create(newCategory) }.await()
                if (res != null) {
                    resetAllUiState()
                    _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Added Category Successfully!"))
                } else {
                    _categoryChannel.send(CategoryEvent.ShowErrorMessage("Adding Category Failed. Please try again later."))
                }
            } else {
                _categoryChannel.send(CategoryEvent.ShowErrorMessage("Please fill the form."))
            }
        }
    }

    fun onUploadImageClicked(imgCategory: Uri) = viewModelScope.launch {
        _categoryChannel.send(CategoryEvent.ShowLoadingBar)
        val uploadedImage = async { categoryRepository.uploadImage(imgCategory) }.await()
        updateFileName(uploadedImage.fileName)
        updateImageUrl(uploadedImage.url)
    }

    private fun resetAllUiState() = viewModelScope.launch {
        categoryId = ""
        categoryName = ""
        updateFileName("")
        updateImageUrl("")
    }

    private fun isFormValid(): Boolean {
        return categoryName.isNotBlank() &&
            fileName.value!!.isNotBlank() &&
            imageUrl.value!!.isNotBlank()
    }

    sealed class CategoryEvent {
        object ShowLoadingBar : CategoryEvent()
        data class NavigateBackWithMessage(val msg: String) : CategoryEvent()
        data class ShowErrorMessage(val msg: String) : CategoryEvent()
    }
}
