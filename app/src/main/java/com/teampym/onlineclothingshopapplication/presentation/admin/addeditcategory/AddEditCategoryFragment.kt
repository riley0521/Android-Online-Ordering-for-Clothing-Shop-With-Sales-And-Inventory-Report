package com.teampym.onlineclothingshopapplication.presentation.admin.addeditcategory

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
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class AddEditCategoryFragment : Fragment(R.layout.fragment_add_edit_category) {

    private lateinit var binding: FragmentAddEditCategoryBinding

    private val args by navArgs<AddEditCategoryFragmentArgs>()

    private val viewModel by viewModels<AddEditCategoryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditCategoryBinding.bind(view)

        val category = args.category
        val isEditMode = args.editMode

        if (category != null) {
            viewModel.categoryId = category.id
            viewModel.categoryName = category.name
            viewModel.imageUrl = category.imageUrl
        }

        binding.apply {
            etCategoryName.setText(viewModel.categoryName)
            tvFileName.text = viewModel.fileName
            tvImageUrl.text = viewModel.imageUrl

            tvFileName.isVisible = viewModel.fileName.isNotBlank()
            tvImageUrl.isVisible = viewModel.imageUrl.isNotBlank()

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
                TODO("Not yet implemented")
            }

            btnSubmit.setOnClickListener {
                TODO("Not yet implemented")
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.categoryEvent.collectLatest { event ->
                when (event) {
                    is AddEditCategoryViewModel.CategoryEvent.NavigateBackWithMessage -> {
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
                }
            }
        }
    }
}
