package com.teampym.onlineclothingshopapplication.presentation.admin.add_edit_product

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.repository.ProductImageRepository
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepository
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.util.ProductType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val userInformationDao: UserInformationDao,
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val CATEGORY_ID = "category_id"
        private const val PRODUCT_ID = "product_id"
        private const val PRODUCT_NAME = "product_name"
        private const val PRODUCT_DESCRIPTION = "product_description"
        private const val PRODUCT_FILE_NAME = "product_file_name"
        private const val PRODUCT_IMAGE_URL = "product_image_url"
        private const val PRODUCT_PRICE = "product_price"
        private const val PRODUCT_TYPE = "product_type"
        private const val PRODUCT_IMAGE_LIST = "product_image_list"
        private const val SELECTED_PRODUCT_IMAGE = "selected_product_image"
        private const val ADDITIONAL_IMAGES = "additional_images"
    }

    var categoryId = state.get(CATEGORY_ID) ?: ""
        set(value) {
            field = value
            state.set(CATEGORY_ID, value)
        }

    var productId = state.get(PRODUCT_ID) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_ID, value)
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
            state.set(PRODUCT_PRICE, value)
        }

    var productType = state.get(PRODUCT_TYPE) ?: ProductType.HOODIES.name
        set(value) {
            field = value
            state.set(PRODUCT_TYPE, value)
        }

    val selectedProductImage = state.getLiveData<Uri>(SELECTED_PRODUCT_IMAGE, null)

    var fileName = state.get(PRODUCT_FILE_NAME) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_FILE_NAME, value)
        }

    var imageUrl = state.get(PRODUCT_IMAGE_URL) ?: ""
        set(value) {
            field = value
            state.set(PRODUCT_IMAGE_URL, value)
        }

    val imageList: MutableLiveData<MutableList<ProductImage>> =
        state.getLiveData(PRODUCT_IMAGE_LIST, mutableListOf())

    val additionalImageList: MutableLiveData<MutableList<Uri>> =
        state.getLiveData(ADDITIONAL_IMAGES, mutableListOf())

    private val _addEditProductChannel = Channel<AddEditProductEvent>()
    val addEditProductEvent = _addEditProductChannel.receiveAsFlow()

    private fun updateImageList(list: List<ProductImage>) {
        imageList.postValue(list.toMutableList())
    }

    fun updateAdditionalImages(list: List<Uri>) {
        additionalImageList.postValue(list.toMutableList())
    }

    private fun resetAllUiState() {
        categoryId = ""
        productDesc = ""
        productPrice = 0.0
        productType = ProductType.HOODIES.name
        selectedProductImage.postValue(null)
        fileName = ""
        imageUrl = ""
        updateImageList(listOf())
        updateAdditionalImages(listOf())
    }

    private fun isFormValid(): Boolean {
        return categoryId.isNotBlank() &&
            productName.isNotBlank() &&
            productPrice > 0.0 &&
            productType.isNotBlank() &&
            fileName.isNotBlank() &&
            imageUrl.isNotBlank() &&
            imageList.value!!.isNotEmpty()
    }

    fun fetchProductImages(productId: String) = viewModelScope.launch {
        val images = productImageRepository.getAll(productId)
        updateImageList(images)
    }

    fun onSubmitClicked(product: Product?, isEditMode: Boolean) = viewModelScope.launch {
        val userSession = preferencesManager.preferencesFlow.first()
        val userInformation = userInformationDao.getCurrentUser(userSession.userId)

        if (isEditMode && product != null && categoryId.isNotBlank()) {

            // Upload image to cloud and pass new value
            // of fileName and imageUrl to product object
            // we need to do this first to pass the check
            async { uploadProductImage() }.await()
            async { uploadAdditionalImages() }.await()

            if (isFormValid()) {
                val updatedProduct = product.copy(
                    categoryId = categoryId,
                    name = productName,
                    description = productDesc,
                    price = productPrice,
                    type = productType,
                    fileName = fileName,
                    imageUrl = imageUrl
                )

                val res = async {
                    productRepository.update(
                        username = "${userInformation?.firstName} ${userInformation?.lastName}",
                        updatedProduct
                    )
                }.await()
                if (res) {
                    val updatedProductImages = async {
                        productImageRepository.updateAll(imageList.value!!)
                    }.await()

                    if (updatedProductImages) {
                        resetAllUiState()

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

            // Upload image to cloud and pass new value
            // of fileName and imageUrl to product object
            // we need to do this first to pass the check
            async { uploadProductImage() }.await()
            async { uploadAdditionalImages() }.await()

            if (isFormValid()) {
                val newProd = Product(
                    categoryId = categoryId,
                    name = productName,
                    description = productDesc,
                    fileName = fileName,
                    imageUrl = imageUrl,
                    price = productPrice,
                    type = productType,
                    dateAdded = Utils.getTimeInMillisUTC()
                )
                val res = async {
                    productRepository.create(
                        username = "${userInformation?.firstName} ${userInformation?.lastName}",
                        newProd
                    )
                }.await()
                if (res != null) {
                    productId = res.productId
                    imageList.value!!.map {
                        it.productId = res.productId
                    }

                    updateImageList(imageList.value!!)
                    val resultList = async {
                        productImageRepository.insertAll(imageList.value!!)
                    }.await()

                    if (resultList) {
                        resetAllUiState()

                        _addEditProductChannel.send(
                            AddEditProductEvent.NavigateToAddInvWithMessage(
                                "Product Added Successfully!\n" +
                                    "Now you need to add the first size/inventory of this product"
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

    private fun uploadProductImage() = viewModelScope.launch {
        if (fileName.isBlank() && productId.isBlank()) {
            if (selectedProductImage.value != null) {
                val uploadedImage = async {
                    productRepository.uploadImage(selectedProductImage.value!!)
                }.await()
                fileName = uploadedImage.fileName
                imageUrl = uploadedImage.url
            }
        } else {
            val res = productRepository.deleteImage(fileName)
            if (res) {
                if (selectedProductImage.value != null) {
                    val uploadedImage = async {
                        productRepository.uploadImage(selectedProductImage.value!!)
                    }.await()
                    fileName = uploadedImage.fileName
                    imageUrl = uploadedImage.url
                }
            }
        }
    }

    private fun uploadAdditionalImages() = viewModelScope.launch {
        if (additionalImageList.value!!.isNotEmpty()) {
            val productImageList = async {
                productImageRepository.uploadImages(additionalImageList.value!!)
            }.await()

            // Empty list of Uri
            updateAdditionalImages(listOf())

            // Update imageList <Product> to trigger observer in UI
            updateImageList(productImageList)
        }
    }

    fun onRemoveProductImageClicked(isEditMode: Boolean, position: Int) =
        viewModelScope.launch {
            if (isEditMode && imageList.value!!.isNotEmpty()) {
                val res =
                    async { productImageRepository.delete(imageList.value!![position]) }.await()
                if (res) {
                    _addEditProductChannel.send(
                        AddEditProductEvent.NotifyAdapterWithMessage(
                            "Product image deleted successfully",
                            position
                        )
                    )
                }
            } else {
                additionalImageList.value!!.removeAt(position)
                _addEditProductChannel.send(
                    AddEditProductEvent.NotifyAdapterWithMessage(
                        "Product image deleted successfully",
                        position
                    )
                )
            }
        }

    fun onDeleteAllAdditionalImagesClicked() = viewModelScope.launch {
        if (imageList.value!!.isNotEmpty()) {
            val res = productImageRepository.deleteAll(imageList.value!!)
            if (res) {
                updateImageList(listOf())
                _addEditProductChannel.send(
                    AddEditProductEvent.ShowSuccessMessage(
                        "All product images successfully deleted."
                    )
                )
            } else {
                _addEditProductChannel.send(
                    AddEditProductEvent.ShowErrorMessage(
                        "Deleting all product images failed. Please wait for a while before trying again."
                    )
                )
            }
        }
    }

    sealed class AddEditProductEvent {
        object ShowLoadingBar : AddEditProductEvent()
        data class NavigateBackWithMessage(val msg: String) : AddEditProductEvent()
        data class NavigateToAddInvWithMessage(val msg: String) : AddEditProductEvent()
        data class ShowSuccessMessage(val msg: String) : AddEditProductEvent()
        data class ShowErrorMessage(val msg: String) : AddEditProductEvent()
        data class NotifyAdapterWithMessage(val msg: String, val position: Int) :
            AddEditProductEvent()
    }
}
