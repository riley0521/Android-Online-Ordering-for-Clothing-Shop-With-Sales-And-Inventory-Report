package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderDetailListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var loadingDialog: LoadingDialog

    private var userInfo: UserInformation? = null

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderDetailListBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        viewModel.updateOrder(args.order)

        setupViews()

        lifecycleScope.launchWhenStarted {
            viewModel.userFlow.collectLatest { user ->
                if (user != null) {
                    userInfo = user

                    adapter = OrderDetailListAdapter(
                        this@OrderDetailListFragment,
                        requireContext(),
                        user
                    )
                    adapter.submitList(viewModel.order?.orderDetailList)

                    if (user.userType == UserType.ADMIN.name) {
                        binding.orderBanner.isVisible = true
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupViews() {
        binding.apply {
            val order = viewModel.order
            order?.let { o ->
                // Set details of the order object again here
                tvOrderId.text = o.id
                tvUsername.text = o.deliveryInformation.name

                val totalCostStr = "$" + o.totalCost
                tvTotalCost.text = totalCostStr

                if (o.suggestedShippingFee > 0.0) {
                    val shippingFee = "$" + o.suggestedShippingFee
                    tvSuggestedSf.text = shippingFee

                    val grandTotalStr = "$" + o.totalPaymentWithShippingFee
                    tvGrandTotal.text = grandTotalStr
                } else {
                    labelShippingFee.isVisible = false
                    tvSuggestedSf.isVisible = false

                    labelGrandTotal.isVisible = false
                    tvGrandTotal.isVisible = false
                }

                if (o.isUserAgreedToShippingFee) {
                    tvUserAgreedToSf.text = "Yes"
                } else {
                    labelUserAgreedToSf.isVisible = false
                    tvUserAgreedToSf.isVisible = false
                }

                val completeAddress = "${o.deliveryInformation.streetNumber} " +
                    "${o.deliveryInformation.city}, " +
                    "${o.deliveryInformation.province}, " +
                    "${o.deliveryInformation.region}, " +
                    o.deliveryInformation.postalCode
                tvDeliveryAddress.text = completeAddress

                tvStatus.text = o.status
                tvNumberOfItems.text = o.orderDetailList.count().toString()

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = o.dateOrdered
                calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val formattedDate =
                    SimpleDateFormat("MM/dd/yyyy hh:mm:ss a").format(calendarDate.time)

                tvDateOrdered.text = formattedDate
                tvAdditionalNote.text = o.additionalNote

                rvOrderDetails.setHasFixedSize(true)
                rvOrderDetails.adapter = adapter
            }
        }
    }

    override fun onExchangeItemClicked(item: OrderDetail) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            loadingDialog.show()
        }

        val isExchangeable = viewModel.checkItemIfExchangeable(item)

        withContext(Dispatchers.Main) {
            loadingDialog.dismiss()

            if (isExchangeable) {
                Toast.makeText(
                    requireContext(),
                    "item ${item.product.productId} is exchangeable",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "item ${item.product.productId} clicked",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // TODO(Navigate to add review fragment)
    override fun onAddReviewClicked(item: OrderDetail) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            loadingDialog.show()
        }

        val canAddReview = viewModel.checkItemIfCanAddReview(item)

        withContext(Dispatchers.Main) {
            loadingDialog.dismiss()

            if (canAddReview) {
                userInfo?.let { u ->
                    val action = OrderDetailListFragmentDirections
                        .actionOrderDetailListFragmentToAddReviewFragment(
                            item,
                            u,
                            item.product.name
                        )
                    findNavController().navigate(action)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    "$item ${item.product.productId} clicked",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
