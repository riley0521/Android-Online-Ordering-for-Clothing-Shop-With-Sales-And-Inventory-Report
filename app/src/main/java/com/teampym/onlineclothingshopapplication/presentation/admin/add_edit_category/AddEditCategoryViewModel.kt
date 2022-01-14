package com.teampym.onlineclothingshopapplication.presentation.admin.add_edit_category

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager,
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

    val selectedImage = state.getLiveData<Uri>(SELECTED_IMAGE, null)

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

    fun onSubmitClicked(category: Category?, isEditMode: Boolean) = viewModelScope.launch {
        val userSession = preferencesManager.preferencesFlow.first()
        val userInformation = userInformationDao.getCurrentUser(userSession.userId)

        if (isEditMode && category != null) {

            // Upload image to cloud and pass new value
            // of fileName and imageUrl to category object
            // we need to do this first to pass the check
            uploadImage()

            if (isFormValid()) {
                val updatedCategory = category.copy(
                    name = categoryName,
                    fileName = fileName,
                    imageUrl = imageUrl
                )

                val res = categoryRepository.update(
                    username = "${userInformation?.firstName} ${userInformation?.lastName}",
                    updatedCategory
                )
                if (res) {
                    resetAllUiState()
                    _categoryChannel.send(CategoryEvent.NavigateBackWithMessage("Updated Category Successfully!"))
                } else {
                    _categoryChannel.send(CategoryEvent.ShowErrorMessage("Updating Category Failed. Please try again later."))
                }
            } else {
                _categoryChannel.send(CategoryEvent.ShowErrorMessage("Please fill the form."))
            }
        } else {

            // Upload image to cloud and pass new value
            // of fileName and imageUrl to category object
            // we need to do this first to pass the check
            uploadImage()

            if (isFormValid()) {
                val newCategory = Category(
                    categoryName,
                    fileName,
                    imageUrl,
                    dateAdded = Utils.getTimeInMillisUTC()
                )
                val res = categoryRepository.create(
                    username = "${userInformation?.firstName} ${userInformation?.lastName}",
                    newCategory
                )
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

    private suspend fun uploadImage() {
        if (fileName.isBlank()) {
            if (selectedImage.value != null) {
                val uploadedImage = categoryRepository.uploadImage(selectedImage.value!!)
                fileName = uploadedImage.fileName
                imageUrl = uploadedImage.url
            }
        } else {
            if (selectedImage.value != null) {
                categoryRepository.deleteImage(fileName)
                val uploadedImage = categoryRepository.uploadImage(selectedImage.value!!)
                fileName = uploadedImage.fileName
                imageUrl = uploadedImage.url
            }
        }
    }

    private fun resetAllUiState() = viewModelScope.launch {
        categoryId = ""
        categoryName = ""
        selectedImage.postValue(null)
        fileName = ""
        imageUrl = ""
    }

    private fun isFormValid(): Boolean {
        return categoryName.isNotBlank() &&
            fileName.isNotBlank() &&
            imageUrl.isNotBlank()
    }

    sealed class CategoryEvent {
        data class NavigateBackWithMessage(val msg: String) : CategoryEvent()
        data class ShowErrorMessage(val msg: String) : CategoryEvent()
    }
}
