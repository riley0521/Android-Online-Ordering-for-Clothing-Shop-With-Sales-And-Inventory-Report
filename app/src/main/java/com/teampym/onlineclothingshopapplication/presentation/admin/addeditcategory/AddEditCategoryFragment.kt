package com.teampym.onlineclothingshopapplication.presentation.admin.addeditcategory

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_add_edit_category.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val SELECT_FROM_GALLERY_REQUEST = 2424

@AndroidEntryPoint
class AddEditCategoryFragment : Fragment(R.layout.fragment_add_edit_category) {

    private lateinit var binding: FragmentAddEditCategoryBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<AddEditCategoryFragmentArgs>()

    private val viewModel by viewModels<AddEditCategoryViewModel>()

    private var selectedCategoryImage: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditCategoryBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val category = args.category
        val isEditMode = args.editMode

        if (category != null) {
            viewModel.categoryId = category.id
            viewModel.categoryName = category.name
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.updateImageUrl(category.imageUrl)
            }
        }

        viewModel.fileName.observe(viewLifecycleOwner) {
            if (it.isNotBlank()) {
                binding.tvFileName.text = it
                binding.tvFileName.isVisible = true
            }
        }

        viewModel.imageUrl.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()

            if (it.isNotBlank()) {
                binding.tvImageUrl.text = it
                binding.tvImageUrl.isVisible = true
            }
        }

        binding.apply {
            etCategoryName.setText(viewModel.categoryName)
            tvFileName.text = viewModel.fileName.value
            tvImageUrl.text = viewModel.imageUrl.value

            if (viewModel.fileName.value!!.isNotBlank()) {
                tvFileName.visibility = View.VISIBLE
            } else {
                tvFileName.visibility = View.INVISIBLE
            }

            if (viewModel.imageUrl.value!!.isNotBlank()) {
                tvImageUrl.visibility = View.VISIBLE
            } else {
                tvImageUrl.visibility = View.INVISIBLE
            }

            etCategoryName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.categoryName = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSelectImageFromGallery.setOnClickListener {
                // Create an intent to select image from gallery.
                Intent(Intent.ACTION_GET_CONTENT).also {
                    it.type = "image/*"
                    startActivityForResult(it, SELECT_FROM_GALLERY_REQUEST)
                }
            }

            btnSubmit.setOnClickListener {
                loadingDialog.show()
                viewModel.onSubmitClicked(category, isEditMode)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.categoryEvent.collectLatest { event ->
                when (event) {
                    is AddEditCategoryViewModel.CategoryEvent.NavigateBackWithMessage -> {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                    is AddEditCategoryViewModel.CategoryEvent.ShowErrorMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    AddEditCategoryViewModel.CategoryEvent.ShowLoadingBar -> {
                        loadingDialog.show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_FROM_GALLERY_REQUEST) {
            data?.data?.let {

                // Show selected image from gallery to the imageView
                binding.imgCategory.setImageURI(it)

                // Upload the image on background thread while the loading screen is showing...
                viewModel.onUploadImageClicked(it)
            }
        }
    }
}
