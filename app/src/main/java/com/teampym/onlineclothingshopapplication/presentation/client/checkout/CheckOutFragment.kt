package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
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

    var finalUser: UserInformation = UserInformation()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)

        var cartList = emptyList<Cart>()

        adapter = CheckOutAdapter()

        viewModel.cart.observe(viewLifecycleOwner) { cart ->
            adapter.submitList(cart)
            cartList = cart
        }

        binding.apply {
            btnChangeAddress.setOnClickListener {
                findNavController().navigate(R.id.action_global_deliveryInformationFragment)
            }

            btnChangePaymentMethod.setOnClickListener {
                // TODO("Proceed to payment method layout (not made yet?)")
                Toast.makeText(requireContext(), "Change Payment Method layout.", Toast.LENGTH_SHORT).show()
            }

            tvMerchandiseTotal.text = "$${args.cart.totalCost}"

            recyclerFinalItems.setHasFixedSize(true)
            recyclerFinalItems.adapter = adapter
        }

        viewModel.selectedPaymentMethod.observe(viewLifecycleOwner) { paymentMethod ->
            btnPlaceOrder.setOnClickListener {

                // get the final cart and place order
                if (paymentMethod.isNotBlank())
                    viewModel.placeOrder(finalUser.copy(cartList = cartList), paymentMethod)
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            finalUser = user.user.copy(
                deliveryInformationList = user.deliveryInformation,
                notificationTokenList = user.notificationTokens
            )

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
        }

        viewModel.order.observe(viewLifecycleOwner) {
            Log.d(TAG, "$it")
            findNavController().navigate(R.id.action_checkOutFragment_to_categoryFragment)
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser
}
