package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentCheckOutBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "CheckOutFragment"

@AndroidEntryPoint
class CheckOutFragment : Fragment(R.layout.fragment_check_out) {

    private lateinit var binding: FragmentCheckOutBinding

    private val viewModel by viewModels<CheckOutViewModel>()

    private val args by navArgs<CheckOutFragmentArgs>()

    private lateinit var adapter: CheckOutAdapter

    private var finalUser: UserInformation = UserInformation()

    private var selectedDeliveryInformation: DeliveryInformation? = null

    private var paymentMethodEnum = PaymentMethod.COD

    private lateinit var loadingDialog: LoadingDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = CheckOutAdapter()
        adapter.submitList(args.cart.cart)

        binding.apply {
            recyclerFinalItems.setHasFixedSize(true)
            recyclerFinalItems.adapter = adapter

            btnChangeAddress.setOnClickListener {
                findNavController().navigate(R.id.action_global_deliveryInformationFragment)
            }

            // TODO ("Will update soon if I find a way to integrate gcash and paymaya to app.")
//            labelPaymentMethod.setOnClickListener {
//                val action =
//                    CheckOutFragmentDirections.actionCheckOutFragmentToSelectPaymentMethodFragment(
//                        tvPaymentMethod.text.toString()
//                    )
//                findNavController().navigate(action)
//            }
//
//            labelPaymentMethod2.setOnClickListener {
//                val action =
//                    CheckOutFragmentDirections.actionCheckOutFragmentToSelectPaymentMethodFragment(
//                        tvPaymentMethod.text.toString()
//                    )
//                findNavController().navigate(action)
//            }

            btnPlaceOrder.setOnClickListener {
                loadingDialog.show()

                // check if the user is verified, get the final info and cartList. Then,
                // Place Order
                val currentUser = getFirebaseUser()
                if (currentUser != null) {
                    if (selectedDeliveryInformation == null) {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            "Please create a delivery information.",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    if (currentUser.isEmailVerified) {
                        Log.d(TAG, "placing order: ${args.cart.cart.size.toLong()}")
                        Log.d(TAG, "placing order: ${args.cart.cart[0].subTotal}")
                        Log.d(TAG, "final user: $finalUser")

                        viewModel.placeOrder(
                            finalUser,
                            args.cart.cart,
                            ""
                        )
                    } else {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            "Please verify your email first to place your order.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    loadingDialog.dismiss()

                    Snackbar.make(
                        requireView(),
                        "Please sign in first.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            val totalCostStr = "$" + String.format("%.2f", args.cart.totalCost)
            tvMerchandiseTotal.text = totalCostStr
            tvTotalPayment.text = totalCostStr
            tvTotalPaymentAgain.text = totalCostStr
        }

        viewModel.selectedPaymentMethod.observe(viewLifecycleOwner) { paymentMethod ->
            paymentMethod?.let { pm ->
                paymentMethodEnum = pm
                val paymentMethodStr = when (pm) {
                    PaymentMethod.GCASH -> R.string.rb_gcash
                    PaymentMethod.PAYMAYA -> R.string.rb_paymaya
                    PaymentMethod.BPI -> R.string.rb_credit_debit_card
                    PaymentMethod.COD -> R.string.rb_cod
                }

                binding.tvPaymentMethod.text = getString(paymentMethodStr)
            }
        }

        lifecycleScope.launchWhenStarted {
            launch {
                val userWithDeliveryInfo = viewModel.userWithDeliveryInfo.first()
                userWithDeliveryInfo?.let {
                    Log.d(TAG, userWithDeliveryInfo.toString())

                    finalUser = userWithDeliveryInfo.user

                    finalUser.defaultDeliveryAddress = userWithDeliveryInfo.deliveryInformation
                        .firstOrNull { it.isPrimary } ?: DeliveryInformation()

                    selectedDeliveryInformation = userWithDeliveryInfo.deliveryInformation
                        .firstOrNull { it.isPrimary } ?: DeliveryInformation()

                    binding.apply {

                        if (finalUser.defaultDeliveryAddress.id.isNotBlank()) {
                            val del = finalUser.defaultDeliveryAddress

                            val contact = if (del.contactNo[0].toString() == "0")
                                del.contactNo.substring(
                                    1,
                                    del.contactNo.length - 1
                                ) else del.contactNo

                            val nameAndContact = "${del.name} | (+63) $contact"
                            tvNameAndContactNo.visibility = View.VISIBLE
                            tvNameAndContactNo.text = nameAndContact

                            val completeAddress = "${del.streetNumber} " +
                                "${del.city}, " +
                                "${del.province}, " +
                                "${del.province}, " +
                                del.postalCode
                            tvCompleteAddress.visibility = View.VISIBLE
                            tvCompleteAddress.text = completeAddress

                            tvNoAddressYet.visibility = View.INVISIBLE
                        }
                    }
                }
            }

            launch {
                viewModel.checkOutEvent.collectLatest { event ->
                    when (event) {
                        is CheckOutViewModel.CheckOutEvent.ShowSuccessfulMessage -> {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT)
                                .show()
                            findNavController().navigate(R.id.action_global_categoryFragment)
                        }
                        is CheckOutViewModel.CheckOutEvent.ShowFailedMessage -> {
                            loadingDialog.dismiss()
                            Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser
}
