package com.teampym.onlineclothingshopapplication.presentation.client.request_return_item

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.NotificationToken
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.Return
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.NotificationData
import com.teampym.onlineclothingshopapplication.data.network.NotificationSingle
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepository
import com.teampym.onlineclothingshopapplication.data.repository.ReturnRepository
import com.teampym.onlineclothingshopapplication.data.util.NOTIFICATION_TOKENS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RequestReturnItemViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val service: FCMService,
    private val returnRepository: ReturnRepository,
    private val orderRepository: OrderRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        const val REASON = "reason"
        const val IMAGE_LIST_URI = "image_list_uri"
        const val UPLOAD_IMAGES = "uploaded_images"
    }

    var reason = state.get(REASON) ?: ""
        set(value) {
            field = value
            state.set(REASON, value)
        }

    var imageListUri: MutableLiveData<MutableList<Uri>> =
        state.getLiveData(IMAGE_LIST_URI, mutableListOf())
    var uploadImages = state.getLiveData(UPLOAD_IMAGES, emptyList<UploadedImage>())

    val returnItem = MutableLiveData<Return>()

    private val _requestChannel = Channel<RequestReturnEvent>()
    val requestReturnEvent = _requestChannel.receiveAsFlow()

    fun fetchReturnItem(orderItemId: String) = viewModelScope.launch {
        returnItem.postValue(returnRepository.getByOrderItemId(orderItemId))
        uploadImages.postValue(returnItem.value?.listOfImage)
    }

    fun updateImageListUri(list: MutableList<Uri>) {
        imageListUri.postValue(list)
    }

    fun removeImage(position: Int) {
        imageListUri.value?.let {
            it.removeAt(position)
            updateImageListUri(it)
        }
    }

    fun onSubmitClicked(orderItem: OrderDetail) = viewModelScope.launch {
        if (imageListUri.value?.isNotEmpty()!! && reason.isNotBlank()) {
            val imageListInDb = returnRepository.uploadImages(imageListUri.value!!)

            val itemToCreate = Return(
                orderItem.id,
                orderItem.product.name,
                reason
            )
            itemToCreate.listOfImage = imageListInDb

            val createdItem = returnRepository.create(itemToCreate)

            if (createdItem) {

                val requestedReturnSuccessfully = orderRepository.returnItem(orderItem)
                if (requestedReturnSuccessfully) {
                    val res = db.collectionGroup(NOTIFICATION_TOKENS_SUB_COLLECTION)
                        .whereEqualTo("userType", UserType.ADMIN.name)
                        .get()
                        .await()

                    if (res != null && res.documents.isNotEmpty()) {
                        val tokenList = mutableListOf<String>()

                        for (doc in res.documents) {
                            val token = doc.toObject<NotificationToken>()!!.copy(id = doc.id)
                            tokenList.add(token.token)
                        }

                        val data = NotificationData(
                            title = "Request to return item",
                            body = "Order item ${orderItem.product.name} (${orderItem.size})",
                            orderItemId = orderItem.id,
                        )

                        val notificationSingle = NotificationSingle(
                            data = data,
                            tokenList = tokenList
                        )

                        service.notifySingleUser(notificationSingle)
                    }

                    resetAllUiState()
                    _requestChannel.send(RequestReturnEvent.ShowSuccessMessage("Submitted request to admins successfully!"))
                }
            } else {
                _requestChannel.send(RequestReturnEvent.ShowSuccessMessage("Submitted request to admins failed. Please try again"))
            }
        } else {
            _requestChannel.send(RequestReturnEvent.ShowSuccessMessage("Please add at least 1 image."))
        }
    }

    private fun resetAllUiState() {
        reason = ""
        state.set(IMAGE_LIST_URI, mutableListOf<Uri>())
        state.set(UPLOAD_IMAGES, mutableListOf<UploadedImage>())
    }

    sealed class RequestReturnEvent {
        data class ShowSuccessMessage(val msg: String) : RequestReturnEvent()
        data class ShowErrorMessage(val msg: String) : RequestReturnEvent()
    }
}
