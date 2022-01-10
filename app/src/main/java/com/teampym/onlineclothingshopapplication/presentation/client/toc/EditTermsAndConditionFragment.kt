package com.teampym.onlineclothingshopapplication.presentation.client.toc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentEditTermsAndConditionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

const val EDIT_TERMS_REQUEST = "edit_terms_request"
const val EDIT_TERMS_RESULT = "edit_terms_result"

@AndroidEntryPoint
class EditTermsAndConditionFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditTermsAndConditionBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<EditTermsAndConditionViewModel>()

    private val args by navArgs<EditTermsAndConditionFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_terms_and_condition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEditTermsAndConditionBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        binding.apply {
            etTermsAndCondition.setText(args.tc.tc)

            btnSubmit.setOnClickListener {
                if (etTermsAndCondition.text.isNotBlank()) {
                    loadingDialog.show()
                    viewModel.onSubmitClicked(args.tc.copy(tc = etTermsAndCondition.text.toString()))
                } else {
                    Snackbar.make(
                        requireView(),
                        "Please enter your terms and conditions",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.editTermsEvent.collectLatest { event ->
                when (event) {
                    is EditTermsAndConditionViewModel.EditTermsEvent.NavigateBackWithMessage -> {
                        loadingDialog.dismiss()
                        setFragmentResult(
                            EDIT_TERMS_REQUEST,
                            bundleOf(EDIT_TERMS_RESULT to true)
                        )
                        findNavController().popBackStack()
                    }
                    is EditTermsAndConditionViewModel.EditTermsEvent.ShowErrorMessage -> {
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
