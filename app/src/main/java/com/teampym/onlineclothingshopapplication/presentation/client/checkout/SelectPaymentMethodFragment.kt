package com.teampym.onlineclothingshopapplication.presentation.client.checkout

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
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.databinding.FragmentSelectPaymentMethodBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

const val SELECT_PAYMENT_REQUEST = "select_payment_request"
const val SELECT_PAYMENT_RESULT = "select_payment_result"

@AndroidEntryPoint
class SelectPaymentMethodFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSelectPaymentMethodBinding

    private val viewModel by viewModels<SelectPaymentMethodViewModel>()

    private val args by navArgs<SelectPaymentMethodFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_payment_method, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSelectPaymentMethodBinding.bind(view)

        viewModel.assignCheckedRadioButton(args.paymentMethod)

        binding.apply {
            btnChoosePaymentMethod.setOnClickListener {
                when {
                    rbCod.isChecked -> {
                        viewModel.updatePaymentMethod(PaymentMethod.COD)
                    }
                    rbCreditDebit.isChecked -> {
                        viewModel.updatePaymentMethod(PaymentMethod.CREDIT_DEBIT)
                    }
                    else -> {
                        Snackbar.make(
                            requireView(),
                            "Please choose a payment method.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.selectPaymentMethodEvent.collectLatest { event ->
                when (event) {
                    SelectPaymentMethodViewModel.SelectPaymentMethodEvent.CheckCashOnDeliveryOption -> {
                        withContext(Dispatchers.Main) {
                            binding.rbCod.isChecked = true
                        }
                    }
                    SelectPaymentMethodViewModel.SelectPaymentMethodEvent.CheckCreditOption -> {
                        withContext(Dispatchers.Main) {
                            binding.rbCreditDebit.isChecked = true
                        }
                    }
                    SelectPaymentMethodViewModel.SelectPaymentMethodEvent.NavigateBack -> {
                        setFragmentResult(
                            SELECT_PAYMENT_REQUEST,
                            bundleOf(SELECT_PAYMENT_RESULT to true)
                        )
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}
