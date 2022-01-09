package com.teampym.onlineclothingshopapplication.presentation.admin.add_edit_category

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val SELECT_CATEGORY_IMG_REQUEST = 2424

@AndroidEntryPoint
class AddEditCategoryFragment : Fragment(R.layout.fragment_add_edit_category) {

    private lateinit var binding: FragmentAddEditCategoryBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<AddEditCategoryFragmentArgs>()

    private val viewModel by viewModels<AddEditCategoryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditCategoryBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val category = args.category
        val isEditMode = args.editMode

        if (category != null) {
            overwriteFields(category)
        }

        viewModel.selectedImage.observe(viewLifecycleOwner) {
            if (it != null) {
                // Show selected image from gallery to the imageView
                Glide.with(requireContext())
                    .load(it)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(binding.imgCategory)
                if (category != null) {
                    overwriteFields(category)
                }
            }
        }

        binding.apply {
            viewModel.categoryId = category?.id ?: ""
            etCategoryName.setText(viewModel.categoryName)

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
                    startActivityForResult(it, SELECT_CATEGORY_IMG_REQUEST)
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

    // This will overwrite the UI state according to the selected category the admin wish to change.
    private fun overwriteFields(category: Category) {
        viewModel.categoryId = category.id
        viewModel.categoryName = category.name

        if (viewModel.selectedImage.value == null) {
            Glide.with(requireContext())
                .load(category.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(binding.imgCategory)
        }

        viewModel.fileName = category.fileName
        viewModel.imageUrl = category.imageUrl
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_CATEGORY_IMG_REQUEST) {
            data?.data?.let {

                viewModel.selectedImage.postValue(it)
            }
        }
    }
}
