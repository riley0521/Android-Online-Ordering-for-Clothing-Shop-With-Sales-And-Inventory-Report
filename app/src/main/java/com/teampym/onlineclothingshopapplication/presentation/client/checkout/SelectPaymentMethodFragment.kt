package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.databinding.FragmentSelectPaymentMethodBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectPaymentMethodFragment : Fragment(R.layout.fragment_select_payment_method) {

    private lateinit var binding: FragmentSelectPaymentMethodBinding

    private val args by navArgs<SelectPaymentMethodFragmentArgs>()

    private val viewModel: CheckOutSharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSelectPaymentMethodBinding.bind(view)

        val paymentMethod = args.paymentMethod

        binding.apply {
            when (paymentMethod) {
                getString(R.string.rb_cod) -> rbCod.isChecked = true
                getString(R.string.rb_gcash) -> rbGCash.isChecked = true
                getString(R.string.rb_paymaya) -> rbPaymaya.isChecked = true
                getString(R.string.rb_credit_debit_card) -> rbCreditDebit.isChecked = true
            }

            btnChoosePaymentMethod.isEnabled = rbGroupPaymentMethods.checkedRadioButtonId != -1
            btnChoosePaymentMethod.setOnClickListener {
                when {
                    rbCod.isChecked -> {
                        viewModel.onPaymentMethodSelected(PaymentMethod.COD)
                    }
                    rbGCash.isChecked -> {
                        viewModel.onPaymentMethodSelected(PaymentMethod.GCASH)
                    }
                    rbPaymaya.isChecked -> {
                        viewModel.onPaymentMethodSelected(PaymentMethod.PAYMAYA)
                    }
                    rbCreditDebit.isChecked -> {
                        viewModel.onPaymentMethodSelected(PaymentMethod.BPI)
                    }
                }
                findNavController().popBackStack()
            }
        }
    }
}
