package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
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

    private var totalCost: Double = 0.0
    private var shippingFee: Double = 0.0

    private var totalWeightInKg: Double = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = CheckOutAdapter(requireContext())
        adapter.submitList(args.cart.cart)

        args.cart.cart.forEach {
            totalWeightInKg += it.totalWeight
        }

        viewModel.fetchSelectPaymentMethod()

        binding.apply {
            recyclerFinalItems.setHasFixedSize(true)
            recyclerFinalItems.adapter = adapter

            btnChangeAddress.setOnClickListener {
                findNavController().navigate(R.id.action_global_deliveryInformationFragment)
            }

            cardView.setOnClickListener {
                setFragmentResultListener(SELECT_PAYMENT_REQUEST) { _, bundle ->
                    val result = bundle.getBoolean(SELECT_PAYMENT_RESULT)
                    Log.d(TAG, result.toString())
                    if (result) {
                        viewModel.fetchSelectPaymentMethod()
                    }
                }

                val action =
                    CheckOutFragmentDirections.actionCheckOutFragmentToSelectPaymentMethodFragment(
                        paymentMethodEnum
                    )
                findNavController().navigate(action)
            }

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
                        viewModel.placeOrder(
                            finalUser,
                            args.cart.cart,
                            etAdditionalNote.text.toString(),
                            paymentMethodEnum,
                            shippingFee
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

            tvMerchandiseTotal.text = getString(
                R.string.placeholder_price,
                args.cart.totalCost
            )
        }

        viewModel.selectedPaymentMethod.observe(viewLifecycleOwner) { paymentMethod ->
            paymentMethod?.let { pm ->
                paymentMethodEnum = pm
                val paymentMethodStr = when (pm) {
                    PaymentMethod.COD -> R.string.rb_cod
                    PaymentMethod.CREDIT_DEBIT -> R.string.rb_credit_debit_card
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
                        .firstOrNull { it.isDefaultAddress } ?: DeliveryInformation()

                    selectedDeliveryInformation = userWithDeliveryInfo.deliveryInformation
                        .firstOrNull { it.isDefaultAddress } ?: DeliveryInformation()

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

                            shippingFee = when (del.region.lowercase()) {
                                "metro manila" -> 80.0
                                "mindanao" -> 175.0
                                "north luzon" -> 120.0
                                "south luzon" -> 120.0
                                "visayas" -> 150.0
                                else -> 0.0
                            }

                            tvShippingFee.text = getString(
                                R.string.placeholder_price,
                                shippingFee
                            )

                            totalCost = args.cart.totalCost + shippingFee
                            tvTotalPaymentAgain.text = getString(
                                R.string.placeholder_price,
                                totalCost
                            )
                        }
                    }
                }
            }

            launch {
                viewModel.checkOutEvent.collectLatest { event ->
                    when (event) {
                        is CheckOutViewModel.CheckOutEvent.ShowSuccessfulMessage -> {
                            loadingDialog.dismiss()

                            if (paymentMethodEnum == PaymentMethod.CREDIT_DEBIT) {
                                Log.d(TAG, "Total Cost to pay: $totalCost")
                            }

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
