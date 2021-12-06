package com.teampym.onlineclothingshopapplication.presentation.client.others

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentCancelReasonDialogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

const val CANCEL_REASON_REQUEST = "cancel_reason_request"
const val CANCEL_REASON_RESULT = "cancel_reason_result"

@AndroidEntryPoint
class CancelReasonDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentCancelReasonDialogBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<CancelReasonDialogFragmentArgs>()

    private val viewModel by viewModels<CancelReasonViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cancel_reason_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCancelReasonDialogBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val order = args.order

        binding.apply {
            etCancelReason.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    etCancelReason.setText(viewModel.cancelReason)
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.cancelReason = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                viewModel.cancelOrderAdmin(order, viewModel.cancelReason)
                loadingDialog.show()
            }
        }

        viewModel.isSuccessful.observe(viewLifecycleOwner) {
            if (it) {
                loadingDialog.dismiss()
                setFragmentResult(
                    CANCEL_REASON_REQUEST,
                    bundleOf(CANCEL_REASON_RESULT to "Canceled Order Successfully!")
                )
                findNavController().popBackStack()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.userFlow.collectLatest { user ->
                if (user != null) {
                    viewModel.updateUserType(user.userType)
                }
            }
        }
    }
}
