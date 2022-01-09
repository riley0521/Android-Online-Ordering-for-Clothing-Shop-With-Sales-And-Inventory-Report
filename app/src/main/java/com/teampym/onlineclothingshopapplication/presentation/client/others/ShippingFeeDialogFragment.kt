package com.teampym.onlineclothingshopapplication.presentation.client.others

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentShippingFeeDialogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val SHIPPING_FEE_REQUEST = "shipping_fee_request"
const val SHIPPING_FEE_RESULT = "shipping_fee_result"

@AndroidEntryPoint
class ShippingFeeDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentShippingFeeDialogBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<ShippingFeeDialogFragmentArgs>()

    private val viewModel by viewModels<ShippingFeeInputViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shipping_fee_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShippingFeeDialogBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val order = args.order

        binding.apply {
            etShippingFee.addTextChangedListener(object : TextWatcher {
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
                        viewModel.shippingFee = s.toString().toDouble()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                if (viewModel.shippingFee > 0.0) {
                    loadingDialog.show()
                    viewModel.submitSuggestedShippingFee(order, viewModel.shippingFee)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please insert a valid shipping fee.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            lifecycleScope.launchWhenStarted {
                launch {
                    val user = viewModel.userFlow.first()
                    if (user != null) {
                        viewModel.updateUserType(user.userType)
                    }
                }

                launch {
                    viewModel.otherDialogEvent.collectLatest { event ->
                        when (event) {
                            OtherDialogFragmentEvent.NavigateBack -> {
                                loadingDialog.dismiss()

                                // Go back to the parent fragment with result
                                setFragmentResult(
                                    SHIPPING_FEE_REQUEST,
                                    bundleOf(SHIPPING_FEE_RESULT to "Submitted suggested shipping fee successfully!")
                                )
                                findNavController().popBackStack()
                            }
                            is OtherDialogFragmentEvent.ShowErrorMessage -> {
                                loadingDialog.dismiss()

                                Toast.makeText(
                                    requireContext(),
                                    event.msg,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
}
