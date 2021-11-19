package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
import com.teampym.onlineclothingshopapplication.databinding.FragmentCheckOutBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "CheckOutFragment"

@AndroidEntryPoint
class CheckOutFragment : Fragment(R.layout.fragment_check_out) {

    private lateinit var binding: FragmentCheckOutBinding

    private val viewModel: CheckOutSharedViewModel by activityViewModels()

    private val args by navArgs<CheckOutFragmentArgs>()

    private lateinit var adapter: CheckOutAdapter

    private var finalUser: UserInformation = UserInformation()

    private lateinit var paymentMethodEnum: PaymentMethod

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)

        adapter = CheckOutAdapter()

        viewModel.finalCartList.observe(viewLifecycleOwner) { cart ->
            adapter.submitList(cart)
            finalUser.cartList = cart
            binding.recyclerFinalItems.setHasFixedSize(true)
            binding.recyclerFinalItems.adapter = adapter
        }

        binding.apply {
            btnChangeAddress.setOnClickListener {
                findNavController().navigate(R.id.action_global_deliveryInformationFragment)
            }

            labelPaymentMethod.setOnClickListener {
                val action =
                    CheckOutFragmentDirections.actionCheckOutFragmentToSelectPaymentMethodFragment(
                        tvPaymentMethod.text.toString()
                    )
                findNavController().navigate(action)
            }

            labelPaymentMethod2.setOnClickListener {
                val action =
                    CheckOutFragmentDirections.actionCheckOutFragmentToSelectPaymentMethodFragment(
                        tvPaymentMethod.text.toString()
                    )
                findNavController().navigate(action)
            }

            btnPlaceOrder.setOnClickListener {
                Log.d(TAG, "TITE")
                // check if the user is verified, get the final info and cartList. Then,
                // Place Order
                val currentUser = getFirebaseUser()
                currentUser?.let {
                    Log.d(TAG, it.toString())
                }
                // Check if the user is signed based on the documentation.
                if (currentUser != null) {
                    if (currentUser.isEmailVerified) {
                        viewModel.placeOrder(
                            finalUser,
                            finalUser.cartList,
                            paymentMethodEnum.name
                        )
                    } else {
                        Snackbar.make(
                            requireView(),
                            "Please verify your email first to place your order.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                } else {
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

        lifecycleScope.launchWhenStarted {
            viewModel.checkOutEvent.collectLatest { event ->
                when (event) {
                    is CheckOutSharedViewModel.CheckOutEvent.ShowSuccessfulMessage -> {
                        Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT)
                            .show()
                        findNavController().navigate(R.id.action_checkOutFragment_to_categoryFragment)
                    }
                    is CheckOutSharedViewModel.CheckOutEvent.ShowFailedMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
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

        viewModel.userWithDeliveryInfo.observe(viewLifecycleOwner) { userWithDeliveryInfo ->
            userWithDeliveryInfo?.let { user ->
                Log.d(TAG, user.toString())

                finalUser = user.user.copy(
                    deliveryInformationList = user.deliveryInformation,
                )

                val defaultDeliveryInfo = user.deliveryInformation.firstOrNull { it.isPrimary }
                binding.apply {
                    defaultDeliveryInfo?.let { del ->
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
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser
}
