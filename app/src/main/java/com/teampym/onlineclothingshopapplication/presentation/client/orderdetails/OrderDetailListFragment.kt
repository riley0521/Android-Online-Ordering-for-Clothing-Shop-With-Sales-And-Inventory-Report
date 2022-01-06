package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderDetailListBinding
import com.teampym.onlineclothingshopapplication.presentation.client.addreview.ADD_REVIEW_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.addreview.ADD_REVIEW_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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

        lifecycleScope.launchWhenStarted {
            val user = viewModel.userFlow.first()
            if (user != null) {
                userInfo = user

                adapter = OrderDetailListAdapter(
                    this@OrderDetailListFragment,
                    requireContext(),
                    user
                )

                if (user.userType == UserType.ADMIN.name) {
                    binding.orderBanner.isVisible = true
                }

                setupViews()
            }
        }
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun setupViews() = CoroutineScope(Dispatchers.Main).launch {
        viewModel.order.observe(viewLifecycleOwner) { order ->
            binding.apply {
                order?.let { o ->
                    adapter.submitList(o.orderDetailList)

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
                    rvOrderDetails.layoutManager = LinearLayoutManager(requireContext())
                    rvOrderDetails.adapter = adapter
                }
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
                userInfo?.let {
                    setFragmentResultListener(ADD_REVIEW_REQUEST) { _, bundle ->
                        val result = bundle.getBoolean(ADD_REVIEW_RESULT)
                        if (result) {
                            Snackbar.make(
                                requireView(),
                                "Review submitted successfully.",
                                Snackbar.LENGTH_SHORT
                            ).show()

                            viewModel.fetchOrderDetails()
                        }
                    }

                    val action = OrderDetailListFragmentDirections
                        .actionOrderDetailListFragmentToAddReviewFragment(
                            item,
                            userInfo!!,
                            item.product.name
                        )
                    findNavController().navigate(action)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    "item ${item.product.productId} clicked",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
