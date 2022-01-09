package com.teampym.onlineclothingshopapplication.presentation.admin.add_edit_product

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.DELETE_ALL_ADDITIONAL_IMAGES
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.SELECT_MULTIPLE_ADDITIONAL_IMAGES
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val SELECT_PRODUCT_IMAGE_FROM_GALLERY_REQUEST = 2425
private const val SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST = 2426

private const val TAG = "AddEditProductFragment"

@AndroidEntryPoint
class AddEditProductFragment :
    Fragment(R.layout.fragment_add_edit_product),
    ProductImageAdapter.ProductImageListener {

    private lateinit var binding: FragmentAddEditProductBinding

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var adapter: ProductImageAdapter

    private val args by navArgs<AddEditProductFragmentArgs>()

    private val viewModel by viewModels<AddEditProductViewModel>()

    private var isEditMode: Boolean = false

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditProductBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val categoryId = args.categoryId
        val product = args.product
        isEditMode = args.editMode

        if (categoryId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Please select a category first " +
                    "where you want to add a new product",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        }

        if (product != null) {
            overwriteFields(product)
        }

        viewModel.selectedProductImage.observe(viewLifecycleOwner) {
            if (it != null) {
                Glide.with(requireContext())
                    .load(it)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(binding.imgProduct)

                if (product != null) {
                    overwriteFields(product)
                }
            }
        }

        viewModel.imageList.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()
            Log.d(TAG, "imageList: $it")

            if (it.isNotEmpty()) {
                adapter = ProductImageAdapter(this, it)

                binding.rvProductImageList.setHasFixedSize(true)
                binding.rvProductImageList.layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
                binding.rvProductImageList.adapter = adapter

                // Change the text of this button.
                binding.btnSelectMultipleProductImage.text = DELETE_ALL_ADDITIONAL_IMAGES
            } else {
                binding.btnSelectMultipleProductImage.text = SELECT_MULTIPLE_ADDITIONAL_IMAGES
            }
        }

        viewModel.additionalImageList.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()
            Log.d(TAG, "additional Images: ${it.size}")

            if (it.isNotEmpty()) {
                Log.d(TAG, "additional Images: ${it[0].path}")

                adapter = ProductImageAdapter(this, it)

                binding.rvProductImageList.setHasFixedSize(true)
                binding.rvProductImageList.layoutManager = LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
                binding.rvProductImageList.adapter = adapter
            }
        }

        binding.apply {

            // Set edit text values when editing a product (product != null)
            viewModel.categoryId = categoryId
            viewModel.productType = args.categoryName
            etProductName.setText(viewModel.productName)
            etProductDescription.setText(viewModel.productDesc)
            if (viewModel.productPrice > 0) {
                etProductPrice.setText(String.format("%.2f", viewModel.productPrice))
            }

            etProductName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.productName = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            etProductDescription.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.productDesc = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            etProductPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString().isNotEmpty()) {
                        viewModel.productPrice = s.toString().toDouble()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSelectImageFromGallery.setOnClickListener {
                // Create an intent to select image from gallery.
                Intent(Intent.ACTION_GET_CONTENT).also {
                    it.type = "image/*"
                    startActivityForResult(it, SELECT_PRODUCT_IMAGE_FROM_GALLERY_REQUEST)
                }
            }

            btnSelectMultipleProductImage.setOnClickListener {
                when {
                    btnSelectMultipleProductImage.text.toString() == SELECT_MULTIPLE_ADDITIONAL_IMAGES -> {
                        Intent(Intent.ACTION_GET_CONTENT).also {
                            it.type = "image/*"
                            it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            startActivityForResult(
                                Intent.createChooser(
                                    it,
                                    SELECT_MULTIPLE_ADDITIONAL_IMAGES
                                ),
                                SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST
                            )
                        }
                    }
                    btnSelectMultipleProductImage.text.toString() == DELETE_ALL_ADDITIONAL_IMAGES -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("DELETE ALL EXISTING PRODUCT IMAGES")
                            .setMessage(
                                "You need to delete all of this first to save cloud storage space.\n" +
                                    "Do you wish to continue?"
                            )
                            .setPositiveButton("Yes") { _, _ ->
                                loadingDialog.show()
                                viewModel.onDeleteAllAdditionalImagesClicked()
                            }.setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }
                }
            }

            btnSubmit.setOnClickListener {
                loadingDialog.show()
                viewModel.onSubmitClicked(product, isEditMode)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.addEditProductEvent.collectLatest { event ->
                when (event) {
                    is AddEditProductViewModel.AddEditProductEvent.NavigateBackWithMessage -> {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                    is AddEditProductViewModel.AddEditProductEvent.NavigateToAddInvWithMessage -> {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_LONG
                        ).show()

                        val action = AddEditProductFragmentDirections
                            .actionAddEditProductFragmentToAddInventoryFragment(
                                productId = viewModel.productId,
                                productName = viewModel.productName
                            )

                        // Remove productId from state after passing it in action variable
                        viewModel.productId = ""
                        viewModel.productName = ""

                        findNavController().navigate(action)
                    }
                    is AddEditProductViewModel.AddEditProductEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is AddEditProductViewModel.AddEditProductEvent.NotifyAdapterWithMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.notifyItemRemoved(event.position)
                    }
                    is AddEditProductViewModel.AddEditProductEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun overwriteFields(product: Product) {
        viewModel.categoryId = product.categoryId
        viewModel.productName = product.name
        viewModel.productDesc = product.description
        viewModel.productPrice = product.price
        viewModel.productType = product.type

        if (viewModel.selectedProductImage.value == null) {
            Glide.with(requireContext())
                .load(product.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(binding.imgProduct)
        }

        viewModel.fileName = product.fileName
        viewModel.imageUrl = product.imageUrl

        viewModel.fetchProductImages(product.productId)
    }

    override fun onRemoveClicked(position: Int) {
        loadingDialog.show()
        viewModel.onRemoveProductImageClicked(isEditMode, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PRODUCT_IMAGE_FROM_GALLERY_REQUEST) {
            data?.data?.let {

                viewModel.selectedProductImage.postValue(it)
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST) {
            data?.clipData?.let {

                // Loop all selected images then add it to the list.
                // Then you can save its state just in case of process death.
                val imageUriList = mutableListOf<Uri>()
                for (pos in 0 until it.itemCount) {
                    imageUriList.add(it.getItemAt(pos).uri)
                }
                viewModel.updateAdditionalImages(imageUriList)
            }
        }
    }
}
