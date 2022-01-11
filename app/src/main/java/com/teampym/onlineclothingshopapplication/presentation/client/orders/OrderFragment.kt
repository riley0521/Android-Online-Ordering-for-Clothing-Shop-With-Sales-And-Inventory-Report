package com.teampym.onlineclothingshopapplication.presentation.client.orders

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.CANCELLED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.COMPLETED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.RETURNED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.SHIPPED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.SHIPPING_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class OrderFragment : Fragment(R.layout.fragment_order) {

    private lateinit var binding: FragmentOrderBinding

    private val viewModel by viewModels<OrderViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderBinding.bind(view)

        binding.apply {
            btnViewShippingOrders.setOnClickListener {
                val action = OrderFragmentDirections.actionOrderFragmentToOrderListFragment(
                    SHIPPING_ORDERS,
                    Status.SHIPPING.name
                )
                findNavController().navigate(action)
            }

            btnViewShippedOrders.setOnClickListener {
                val action = OrderFragmentDirections.actionOrderFragmentToOrderListFragment(
                    SHIPPED_ORDERS,
                    Status.SHIPPED.name
                )
                findNavController().navigate(action)
            }

            btnViewDeliveryOrders.setOnClickListener {
                val action = OrderFragmentDirections.actionOrderFragmentToOrderListFragment(
                    DELIVERY_ORDERS,
                    Status.DELIVERY.name
                )
                findNavController().navigate(action)
            }

            btnViewCompletedOrders.setOnClickListener {
                val action = OrderFragmentDirections.actionOrderFragmentToOrderListFragment(
                    COMPLETED_ORDERS,
                    Status.COMPLETED.name
                )
                findNavController().navigate(action)
            }

            btnViewReturnedOrders.setOnClickListener {
                Snackbar.make(
                    requireView(),
                    "Gagawin pa lang",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            btnViewCancelledOrders.setOnClickListener {
                val action = OrderFragmentDirections.actionOrderFragmentToOrderListFragment(
                    CANCELLED_ORDERS,
                    Status.CANCELED.name
                )
                findNavController().navigate(action)
            }
        }

        lifecycleScope.launchWhenStarted {
            val user = viewModel.userFlow.first()

            if (user == null) {
                Toast.makeText(requireContext(), "Please login first.", Toast.LENGTH_SHORT).show()
                val action = OrderFragmentDirections.actionOrderFragmentToProfileFragment(true)
                findNavController().navigate(action)
            }
        }
    }
}
