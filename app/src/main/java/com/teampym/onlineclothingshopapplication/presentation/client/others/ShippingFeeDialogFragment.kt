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
                    etShippingFee.setText(viewModel.shippingFee.toString())
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.shippingFee = (s.toString()).toBigDecimal()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                if (viewModel.shippingFee > (0).toBigDecimal()) {
                    viewModel.submitSuggestedShippingFee(order, viewModel.shippingFee)
                    loadingDialog.show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please insert a valid shipping fee.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            viewModel.isSuccessful.observe(viewLifecycleOwner) {
                if (it) {
                    loadingDialog.dismiss()
                    setFragmentResult(
                        SHIPPING_FEE_REQUEST,
                        bundleOf(SHIPPING_FEE_RESULT to "Submitted suggested shipping fee successfully!")
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
}
