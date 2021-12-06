package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderDetailListBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

@AndroidEntryPoint
class OrderDetailListFragment :
    Fragment(R.layout.fragment_order_detail_list),
    OrderDetailListAdapter.OrderDetailListener {

    private lateinit var binding: FragmentOrderDetailListBinding

    private val args by navArgs<OrderDetailListFragmentArgs>()

    private val viewModel by viewModels<OrderDetailListViewModel>()

    private lateinit var adapter: OrderDetailListAdapter

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderDetailListBinding.bind(view)

        var order = args.order
        viewModel.updateOrder(order)

        // Reassign order variable in case process death took place.
        order = viewModel.order

        adapter = OrderDetailListAdapter(this)
        adapter.submitList(order.orderDetailList)

        binding.apply {

            // Set details of the order object again here
            tvOrderId.text = order.id
            tvUsername.text = order.deliveryInformation.name

            val totalCostStr = "$" + order.totalCost
            tvTotalCost.text = totalCostStr

            if (order.suggestedShippingFee > 0.0) {
                val shippingFee = "$" + order.suggestedShippingFee
                tvSuggestedSf.text = shippingFee

                val grandTotalStr = "$" + order.totalPaymentWithShippingFee
                tvGrandTotal.text = grandTotalStr
            } else {
                labelShippingFee.isVisible = false
                tvSuggestedSf.isVisible = false

                labelGrandTotal.isVisible = false
                tvGrandTotal.isVisible = false
            }

            if (order.isUserAgreedToShippingFee) {
                tvUserAgreedToSf.text = "Yes"
            } else {
                tvUserAgreedToSf.text = "No"
            }

            tvStatus.text = order.status
            tvNumberOfItems.text = order.orderDetailList.count().toString()

            val calendarDate = Calendar.getInstance()
            calendarDate.timeInMillis = order.dateOrdered
            val formattedDate =
                SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

            tvDateOrdered.text = formattedDate
            tvAdditionalNote.text = order.additionalNote

            rvOrderDetails.setHasFixedSize(true)
            rvOrderDetails.adapter = adapter
        }
    }

    override fun onExchangeItemClicked(item: OrderDetail) {
        Toast.makeText(requireContext(), "item ${item.product.productId} clicked", Toast.LENGTH_SHORT).show()
    }
}
