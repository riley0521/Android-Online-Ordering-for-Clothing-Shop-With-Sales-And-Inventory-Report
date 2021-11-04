package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.PaymentMethod
import com.teampym.onlineclothingshopapplication.databinding.FragmentCheckOutBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_check_out.*

private const val TAG = "CheckOutFragment"

@AndroidEntryPoint
class CheckOutFragment : Fragment(R.layout.fragment_check_out) {

    private lateinit var binding: FragmentCheckOutBinding

    private val viewModel by viewModels<CheckOutViewModel>()

    private val args by navArgs<CheckOutFragmentArgs>()

    private lateinit var adapter: CheckOutAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)

        val cartList = args.cart.cart

        adapter = CheckOutAdapter()

        binding.apply {
            btnPlaceOrder.setOnClickListener {
                // TODO("Complete placing order process.")
            }

            tvMerchandiseTotal.text = "$${args.cart.totalCost}"

            adapter.submitList(cartList)

            recyclerFinalItems.setHasFixedSize(true)
            recyclerFinalItems.adapter = adapter
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            val defaultInfo = user.deliveryInformation.first { it.default }
            binding.apply {
                val contact = if (defaultInfo.contactNo[0].toString() == "0")
                    defaultInfo.contactNo.substring(
                        1,
                        defaultInfo.contactNo.length - 1
                    ) else defaultInfo.contactNo

                val nameAndContact = "${defaultInfo.name} | (+63) $contact"
                tvNameAndContactNo.text = nameAndContact

                val completeAddress = "${defaultInfo.streetNumber} " +
                        "${defaultInfo.city}, " +
                        "${defaultInfo.province}, " +
                        "${defaultInfo.province}, " +
                        defaultInfo.postalCode
                tvCompleteAddress.text = completeAddress
            }

            btnPlaceOrder.setOnClickListener {
                val paymentMethod = tvPaymentMethod.text.toString()

                // create new User Object to pass in the context
                val newUser = UserInformation(
                    firstName = user.user.firstName,
                    lastName = user.user.lastName,
                    birthDate = user.user.lastName,
                    avatarUrl = user.user.avatarUrl,
                    userId = user.user.userId,
                    userType = user.user.userType,
                    deliveryInformationList = user.deliveryInformation,
                    notificationTokenList = user.notificationTokens,
                    cartList = cartList
                )

                when {
                    paymentMethod.contains("COD") -> {
                        // Place Order

                        viewModel.placeOrder(newUser, PaymentMethod.COD.name)
                    }
                    paymentMethod.contains("GCASH") -> {
                        PaymentMethod.GCASH.name
                        // Send to GCASH payment page
                    }
                    paymentMethod.contains("PAYMAYA") -> {
                        PaymentMethod.PAYMAYA.name
                        // Send to PAYMAYA payment page
                    }
                    else -> {
                        PaymentMethod.BPI.name
                        // Send to BPI payment page
                    }
                }

            }

        }

        viewModel.order.observe(viewLifecycleOwner) {
            Log.d(TAG, "$it")
            findNavController().navigate(R.id.action_checkOutFragment_to_categoryFragment)
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser
}