package com.teampym.onlineclothingshopapplication.presentation.admin.add_news

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddNewsBinding
import dagger.hilt.android.AndroidEntryPoint

private const val SELECT_POST_IMG_REQUEST = 1234

@AndroidEntryPoint
class AddNewsFragment : Fragment(R.layout.fragment_add_news) {

    private lateinit var binding: FragmentAddNewsBinding

    private val viewModel by viewModels<AddNewsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddNewsBinding.bind(view)

        viewModel.userSession.observe(viewLifecycleOwner) {
            viewModel.fetchUserInformation(it.userId)
        }

        viewModel.userInformation.observe(viewLifecycleOwner) {
            setupViews(it)
        }
    }

    private fun setupViews(userInformation: UserInformation?) {

        viewModel.image.observe(viewLifecycleOwner) {
            binding.apply {
                it?.let { imageUri ->
                    imgAdditional.setImageURI(imageUri)
                    imgAdditional.isVisible = true

                    btnAddOrRemoveImage.text = getString(R.string.btn_remove_image)
                }
            }
        }

        binding.apply {
            etTitle.setText(viewModel.title)
            etDescription.setText(viewModel.description)
            viewModel.image.value?.let {
                imgAdditional.setImageURI(it)
                imgAdditional.isVisible = true

                btnAddOrRemoveImage.text = getString(R.string.btn_remove_image)
            }

            etTitle.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.title = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing.
                }
            })

            etDescription.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing.
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.description = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing.
                }
            })

            btnAddOrRemoveImage.setOnClickListener {
                if (btnAddOrRemoveImage.text == getString(
                        R.string.btn_add_image
                    )
                ) {
                    // Create an intent to select image from gallery.
                    Intent(Intent.ACTION_GET_CONTENT).also {
                        it.type = "image/*"
                        startActivityForResult(it, SELECT_POST_IMG_REQUEST)
                    }
                } else if (btnAddOrRemoveImage.text == getString(
                        R.string.btn_remove_image
                    )
                ) {
                    viewModel.updateImage(null)
                    imgAdditional.isVisible = false
                    btnAddOrRemoveImage.text = getString(R.string.btn_add_image)

                    Snackbar.make(
                        requireView(),
                        "Image removed.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            btnAddPost.setOnClickListener {
                if (viewModel.isFormValid()) {
                    userInformation?.let { user ->
                        viewModel.onAddPostClicked(
                            user
                        )
                        Toast.makeText(
                            requireContext(),
                            "Adding post...",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    }
                } else {
                    Snackbar.make(
                        requireView(),
                        "Please fill the form.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_POST_IMG_REQUEST) {
            data?.data?.let {

                // Show selected image from gallery to the imageView
                viewModel.updateImage(it)
            }
        }
    }
}
