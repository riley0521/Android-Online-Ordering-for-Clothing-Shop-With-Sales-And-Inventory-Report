package com.teampym.onlineclothingshopapplication.presentation.admin.add_news

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.di.ApplicationScope
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.repository.PostRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddNewsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userInformationDao: UserInformationDao,
    preferencesManager: PreferencesManager,
    private val state: SavedStateHandle,
    @ApplicationScope val appScope: CoroutineScope
) : ViewModel() {

    companion object {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val IMAGE = "image"
    }

    val userSession = preferencesManager.preferencesFlow.asLiveData()
    val userInformation = MutableLiveData<UserInformation>()

    var title = state.get(TITLE) ?: ""
        set(value) {
            field = value
            state.set(TITLE, value)
        }

    var description = state.get(DESCRIPTION) ?: ""
        set(value) {
            field = value
            state.set(DESCRIPTION, value)
        }

    var image = state.getLiveData<Uri>(IMAGE)

    fun fetchUserInformation(userId: String) = viewModelScope.launch {
        userInformation.postValue(userInformationDao.getCurrentUser(userId))
    }

    fun onAddPostClicked(userInformation: UserInformation) = appScope.launch {

        var uploadedImageWithUrl: UploadedImage? = null
        image.value?.let { imageUri ->
            uploadedImageWithUrl = postRepository.uploadImage(imageUri)
        }

        postRepository.insert(
            Post(
                title = title,
                description = description,
                createdBy = "${userInformation.firstName} ${userInformation.lastName}",
                avatarUrl = userInformation.avatarUrl ?: "",
                imageUrl = uploadedImageWithUrl?.url ?: "",
                fileName = uploadedImageWithUrl?.fileName ?: "",
            )
        )
        resetUiState()
    }

    fun isFormValid(): Boolean {
        return title.isNotBlank() && description.isNotBlank()
    }

    fun updateImage(imageUri: Uri?) {
        image.postValue(imageUri)
    }

    private fun resetUiState() {
        title = ""
        description = ""
        updateImage(null)
    }
}
