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
        private const val SELECTED_IMAGE = "selected_image"
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

    var selectedImage = state.get<Uri?>(SELECTED_IMAGE)
        set(value) {
            field = value
            state.set(SELECTED_IMAGE, value)
        }

    val fileName: MutableLiveData<String> = state.getLiveData(FILE_NAME, "")

    val imageUrl: MutableLiveData<String> = state.getLiveData(IMAGE_URL, "")

    fun updateFileName(name: String) {
        fileName.postValue(name)
    }

    fun updateImageUrl(url: String) {
        imageUrl.postValue(url)
    }

    fun onSubmitClicked(category: Category?, isEditMode: Boolean) = viewModelScope.launch {
        if (isEditMode && category != null && categoryId.isNotBlank()) {
            if (isFormValid()) {

                // Upload image to cloud and pass new value
                // of fileName and imageUrl to category object
                async { uploadImage() }.await()
                category.fileName = fileName.value!!
                category.imageUrl = fileName.value!!

                val res = async { categoryRepository.update(category) }.await()
                if (res != null) {
                    resetAllUiState()
                    _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Updated Category Successfully!"))
                } else {
                    _categoryChannel.send(CategoryEvent.ShowErrorMessage("Updating Category Failed. Please try again later."))
                }
            } else {
                _categoryChannel.send(CategoryEvent.ShowErrorMessage("Please fill the form."))
            }
        } else {
            if (isFormValid()) {
                // Upload image to cloud and pass new value
                // of fileName and imageUrl to category object
                async { uploadImage() }.await()

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

    private fun uploadImage() = viewModelScope.launch {
        if (fileName.value!!.isBlank() && categoryId.isBlank()) {
            if (selectedImage != null) {
                val uploadedImage =
                    async { categoryRepository.uploadImage(selectedImage!!) }.await()
                updateFileName(uploadedImage.fileName)
                updateImageUrl(uploadedImage.url)
            }
        } else {
            val res = categoryRepository.deleteImage(fileName.value!!)
            if (res) {
                if (selectedImage != null) {
                    val uploadedImage =
                        async { categoryRepository.uploadImage(selectedImage!!) }.await()
                    updateFileName(uploadedImage.fileName)
                    updateImageUrl(uploadedImage.url)
                }
            }
        }
    }

    private fun resetAllUiState() = viewModelScope.launch {
        categoryId = ""
        categoryName = ""
        selectedImage = null
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
