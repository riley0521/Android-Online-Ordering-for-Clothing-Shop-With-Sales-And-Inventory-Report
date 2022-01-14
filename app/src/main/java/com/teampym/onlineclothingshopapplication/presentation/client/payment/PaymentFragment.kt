package com.teampym.onlineclothingshopapplication.presentation.client.payment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.OrderIntent
import com.paypal.checkout.createorder.UserAction
import com.paypal.checkout.error.OnError
import com.paypal.checkout.order.Amount
import com.paypal.checkout.order.AppContext
import com.paypal.checkout.order.Order
import com.paypal.checkout.order.PurchaseUnit
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentPaymentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class PaymentFragment : Fragment(R.layout.fragment_payment) {

    private lateinit var binding: FragmentPaymentBinding

    private val args by navArgs<PaymentFragmentArgs>()

    private val viewModel by viewModels<PaymentViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPaymentBinding.bind(view)

        binding.payPalButton.setup(
            createOrder = CreateOrder { createOrderActions ->
                val order =
                    Order(
                        intent = OrderIntent.CAPTURE,
                        appContext = AppContext(userAction = UserAction.PAY_NOW),
                        purchaseUnitList =
                        listOf(
                            PurchaseUnit(
                                amount =
                                Amount(
                                    currencyCode = CurrencyCode.PHP,
                                    value = args.totalCost.toString()
                                )
                            )
                        )
                    )
                createOrderActions.create(order)
            },
            onApprove = OnApprove { approval ->
                approval.orderActions.capture { captureOrderResult ->
                    Log.i("CaptureOrder", "CaptureOrderResult: $captureOrderResult")

                    // Update order here
                    viewModel.updateOrder(args.orderId)
                }
            },
            onCancel = OnCancel {
                Log.d("OnCancel", "Buyer canceled the PayPal experience.")

                findNavController().navigate(R.id.action_global_categoryFragment)
            },
            onError = OnError { errorInfo ->
                Log.d("OnError", "Error: $errorInfo")

                findNavController().navigate(R.id.action_global_categoryFragment)
            }
        )

        lifecycleScope.launchWhenStarted {
            viewModel.paymentEvent.collectLatest { event ->
                when (event) {
                    is PaymentViewModel.PaymentEvent.ShowMessage -> {
                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().navigate(R.id.action_global_categoryFragment)
                    }
                }
            }
        }
    }
}
