package com.teampym.onlineclothingshopapplication.presentation.admin.addeditproduct

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageRepository
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.ProductType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val CATEGORY_ID = "category_id"
        private const val PRODUCT_NAME = "product_name"
        private const val PRODUCT_DESCRIPTION = "product_description"
        private const val PRODUCT_FILE_NAME = "product_file_name"
        private const val PRODUCT_IMAGE_URL = "product_image_url"
        private const val PRODUCT_PRICE = "product_price"
        private const val PRODUCT_TYPE = "product_type"
        private const val PRODUCT_IMAGE_LIST = "product_image_list"
    }

    var categoryId = state.get(CATEGORY_ID) ?: ""
        set(value) {
            field = value
            state.set(CATEGORY_ID, value)
        }

    var productName = state.get(PRODUCT_NAME) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_NAME, value)
        }

    var productDesc = state.get(PRODUCT_DESCRIPTION) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_DESCRIPTION, value)
        }

    var productPrice = state.get<Double>(PRODUCT_PRICE) ?: 0.0
        set(value) {
            field = value
            state.set(PRODUCT_FILE_NAME, value)
        }

    var productType = state.get(PRODUCT_TYPE) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_TYPE, value)
        }

    val fileName: MutableLiveData<String> = state.getLiveData(PRODUCT_FILE_NAME, "")

    val imageUrl: MutableLiveData<String> = state.getLiveData(PRODUCT_IMAGE_URL, "")

    val imageList: MutableLiveData<MutableList<ProductImage>> =
        state.getLiveData(PRODUCT_IMAGE_LIST, mutableListOf())

    private val _addEditProductChannel = Channel<AddEditProductEvent>()
    val addEditProductEvent = _addEditProductChannel.receiveAsFlow()

    suspend fun updateFileName(name: String) {
        fileName.postValue(name)
    }

    suspend fun updateImageUrl(url: String) {
        imageUrl.postValue(url)
    }

    suspend fun updateImageList(list: List<ProductImage>) {
        imageList.postValue(list.toMutableList())
    }

    private fun resetAllUiState() = viewModelScope.launch {
        categoryId = ""
        productName = ""
        productDesc = ""
        productPrice = 0.0
        productType = ProductType.HOODIES.name
        updateFileName("")
        updateImageUrl("")
        updateImageList(listOf())
    }

    private fun isFormValid(): Boolean {
        return categoryId.isNotBlank() &&
            productName.isNotBlank() &&
            productPrice > 0.0 &&
            productType.isNotBlank() &&
            fileName.value!!.isNotBlank() &&
            imageUrl.value!!.isNotBlank() &&
            imageList.value!!.isNotEmpty()
    }

    fun onSubmitClicked(product: Product?, isEditMode: Boolean) = viewModelScope.launch {
        if (isEditMode && product != null && categoryId.isNotBlank()) {
            if (isFormValid()) {
                val res = async { productRepository.update(product) }.await()
                if (res) {
                    val updatedProductImages = async {
                        productImageRepository.updateAll(imageList.value!!)
                    }.await()

                    if (updatedProductImages) {
                        _addEditProductChannel.send(
                            AddEditProductEvent.NavigateBackWithMessage(
                                "Product Updated Successfully!"
                            )
                        )
                    }
                }
            } else {
                _addEditProductChannel.send(
                    AddEditProductEvent.ShowErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        } else {
            if (isFormValid()) {
                val newProd = Product(
                    categoryId = categoryId,
                    name = productName,
                    description = productDesc,
                    fileName = fileName.value!!,
                    imageUrl = imageUrl.value!!,
                    price = productPrice,
                    type = productType,
                    dateAdded = Utils.getTimeInMillisUTC()
                )
                val res = async { productRepository.create(newProd) }.await()
                if (res != null) {
                    imageList.value!!.map {
                        it.productId = res.productId
                    }

                    updateImageList(imageList.value!!)

                    val resultList = async {
                        productImageRepository.insertAll(imageList.value!!)
                    }.await()

                    if (resultList) {
                        _addEditProductChannel.send(
                            AddEditProductEvent.NavigateToAddInvWithMessage(
                                "Product Added Successfully!"
                            )
                        )
                    }
                }
            } else {
                _addEditProductChannel.send(
                    AddEditProductEvent.ShowErrorMessage(
                        "Please fill the form."
                    )
                )
            }
        }
    }

    fun onUploadProductImageClicked(imgProduct: Uri) = viewModelScope.launch {
        _addEditProductChannel.send(AddEditProductEvent.ShowLoadingBar)
        val uploadedImage = async { productRepository.uploadImage(imgProduct) }.await()
        updateFileName(uploadedImage.fileName)
        updateImageUrl(uploadedImage.url)
    }

    fun onUploadProductImageListClicked(imgUriList: List<Uri>) = viewModelScope.launch {
        _addEditProductChannel.send(AddEditProductEvent.ShowLoadingBar)
        val productImageList = async { productImageRepository.uploadImages(imgUriList) }.await()
        updateImageList(productImageList)
    }

    fun onRemoveProductImageClicked(isEditMode: Boolean, item: ProductImage, position: Int) =
        viewModelScope.launch {
            if (isEditMode) {
                val res = async { productImageRepository.delete(item) }.await()
                if (res) {
                    _addEditProductChannel.send(
                        AddEditProductEvent.NotifyAdapterWithMessage(
                            "Product image deleted successfully",
                            position
                        )
                    )
                }
            } else {
                imageList.value!!.removeAt(position)
                updateImageList(imageList.value!!)
                _addEditProductChannel.send(
                    AddEditProductEvent.NotifyAdapterWithMessage(
                        "Product image deleted successfully",
                        position
                    )
                )
            }
        }

    sealed class AddEditProductEvent {
        object ShowLoadingBar : AddEditProductEvent()
        data class NavigateBackWithMessage(val msg: String) : AddEditProductEvent()
        data class NavigateToAddInvWithMessage(val msg: String) : AddEditProductEvent()
        data class ShowErrorMessage(val msg: String) : AddEditProductEvent()
        data class NotifyAdapterWithMessage(val msg: String, val position: Int) :
            AddEditProductEvent()
    }
}
