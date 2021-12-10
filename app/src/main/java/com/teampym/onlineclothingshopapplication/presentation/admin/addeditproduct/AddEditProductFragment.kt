package com.teampym.onlineclothingshopapplication.presentation.admin.addeditproduct

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val SELECT_PRODUCT_IMAGE_FROM_GALLERY_REQUEST = 2425
const val SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST = 2426

@AndroidEntryPoint
class AddEditProductFragment :
    Fragment(R.layout.fragment_add_edit_product),
    AdapterView.OnItemSelectedListener,
    ProductImageAdapter.ProductImageListener {

    private lateinit var binding: FragmentAddEditProductBinding

    private lateinit var loadingDialog: LoadingDialog

    private var adapter: ProductImageAdapter? = null

    private val args by navArgs<AddEditProductFragmentArgs>()

    private val viewModel by viewModels<AddEditProductViewModel>()

    private var isEditMode: Boolean = false

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

        viewModel.imageList.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()

            if (it.isNotEmpty()) {
                adapter = ProductImageAdapter(this, it)

                binding.rvProductImageList.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = adapter
                }
                binding.btnSelectMultipleProductImage.text = DELETE_ALL_ADDITIONAL_IMAGES
            } else {
                binding.btnSelectMultipleProductImage.text = SELECT_MULTIPLE_ADDITIONAL_IMAGES
            }
        }

        viewModel.additionalImageList.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()

            if (it.isNotEmpty()) {
                adapter = ProductImageAdapter(this, it)

                binding.rvProductImageList.apply {
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(requireContext())
                    adapter = adapter
                }
            }
        }

        binding.apply {

            ArrayAdapter.createFromResource(
                requireContext(),
                R.array.product_types_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spProductType.adapter = adapter
                // Set selected item when editing a product
                spProductType.setSelection(adapter.getPosition(viewModel.productType))
            }

            spProductType.onItemSelectedListener = this@AddEditProductFragment

            if (btnSelectMultipleProductImage.text.isBlank()) {
                btnSelectMultipleProductImage.text = SELECT_MULTIPLE_ADDITIONAL_IMAGES
            }

            // Set edit text values when editing a product (product != null)
            viewModel.categoryId = categoryId
            etProductName.setText(viewModel.productName)
            etProductDescription.setText(viewModel.productDesc)
            etProductPrice.setText(viewModel.productPrice.toString())

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
                    viewModel.productPrice = s.toString().toDouble()
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
                when (binding.btnSelectImageFromGallery.text) {
                    SELECT_MULTIPLE_ADDITIONAL_IMAGES -> {
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
                    DELETE_ALL_ADDITIONAL_IMAGES -> {
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
                    AddEditProductViewModel.AddEditProductEvent.ShowLoadingBar -> {
                        loadingDialog.show()
                    }
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
                                viewModel.productId
                            )

                        // Remove productId from state after passing it in action variable
                        viewModel.productId = ""

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
                        adapter?.notifyItemRemoved(event.position)
                    }
                    is AddEditProductViewModel.AddEditProductEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun overwriteFields(product: Product) = CoroutineScope(Dispatchers.IO).launch {
        viewModel.categoryId = product.categoryId
        viewModel.productName = product.name
        viewModel.productDesc = product.description
        viewModel.productPrice = product.price
        viewModel.productType = product.type

        Glide.with(requireContext())
            .load(product.imageUrl)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_food)
            .into(binding.imgProduct)

        viewModel.updateFileName(product.fileName)
        viewModel.updateImageUrl(product.imageUrl)
        viewModel.updateImageList(product.productImageList)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.productType = parent?.getItemAtPosition(position).toString()
        Log.d("AddEditProductFragment", viewModel.productType)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Nothing
    }

    override fun onRemoveClicked(position: Int) {
        loadingDialog.show()
        viewModel.onRemoveProductImageClicked(isEditMode, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_PRODUCT_IMAGE_FROM_GALLERY_REQUEST) {
            data?.data?.let {

                // Show selected image from gallery to the imageView
                binding.imgProduct.setImageURI(it)
                viewModel.selectedProductImage = it
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST) {
            data?.clipData?.let {

                // Loop all selected images then add it to the list.
                // Then you can save its state just in case of process death.
                val imageUriList = mutableListOf<Uri>()
                for (pos in 0..it.itemCount) {
                    imageUriList.add(it.getItemAt(pos).uri)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.updateAdditionalImages(imageUriList)
                }
            }
        }
    }
}
