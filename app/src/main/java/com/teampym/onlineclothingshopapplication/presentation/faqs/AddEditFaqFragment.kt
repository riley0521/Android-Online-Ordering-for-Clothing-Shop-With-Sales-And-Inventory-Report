package com.teampym.onlineclothingshopapplication.presentation.faqs

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditFaqBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

const val ADD_EDIT_FAQ_REQUEST = "add_edit_faq_request"
const val ADD_EDIT_FAQ_RESULT = "add_edit_faq_result"

private const val TAG = "AddEditFaqFragment"

@AndroidEntryPoint
class AddEditFaqFragment : Fragment(R.layout.fragment_add_edit_faq) {

    private lateinit var binding: FragmentAddEditFaqBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<AddEditFaqViewModel>()

    private val args by navArgs<AddEditFaqFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditFaqBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Add New FAQ"

        val faq = args.faq

        faq?.let {
            viewModel.question = faq.question
            viewModel.answer = faq.answer

            Log.d(TAG, "onViewCreated: ${viewModel.question}")
            Log.d(TAG, "onViewCreated: ${viewModel.answer}")

            (requireActivity() as AppCompatActivity).supportActionBar?.title = "Edit FAQ"
        }

        binding.apply {
            etQuestion.setText(viewModel.question)
            etAnswer.setText(viewModel.answer)

            etQuestion.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.question = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            etAnswer.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.answer = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                loadingDialog.show()
                if (faq != null) {
                    viewModel.onSubmitClicked(faq, true)
                } else {
                    viewModel.onSubmitClicked(
                        null,
                        false
                    )
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.addEditFaqEvent.collectLatest { event ->
                when (event) {
                    is AddEditFaqViewModel.AddEditFaqEvent.NavigateBackWithMessage -> {
                        loadingDialog.dismiss()
                        setFragmentResult(
                            ADD_EDIT_FAQ_REQUEST,
                            bundleOf(ADD_EDIT_FAQ_RESULT to event.msg)
                        )
                        findNavController().popBackStack()
                    }
                    is AddEditFaqViewModel.AddEditFaqEvent.ShowErrorMessage -> {
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
}
