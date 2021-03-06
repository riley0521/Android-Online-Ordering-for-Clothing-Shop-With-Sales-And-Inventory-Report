package com.teampym.onlineclothingshopapplication.presentation.client.orderdetails

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.teampym.onlineclothingshopapplication.data.room.PaymentMethod
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

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderDetailListBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        if (args.order != null) {
            viewModel.updateOrder(args.order!!)
        }

        lifecycleScope.launchWhenStarted {
            val user = viewModel.userFlow.first()
            if (user != null) {
                viewModel.userInfo.postValue(user)

                if (args.order == null) {
                    viewModel.fetchOrderWithDetailsById(args.orderId, user.userId)
                }

                adapter = OrderDetailListAdapter(
                    this@OrderDetailListFragment,
                    requireContext(),
                    user
                )

                if (user.userType == UserType.ADMIN.name) {
                    binding.generalInfoView.isVisible = true
                    binding.orderInfoView.isVisible = true
                }

                setupViews()
            }
        }

        setHasOptionsMenu(true)
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun setupViews() = CoroutineScope(Dispatchers.Main).launch {
        viewModel.order.observe(viewLifecycleOwner) { order ->
            binding.apply {
                order?.let { o ->
                    adapter.submitList(o.orderDetailList)

                    // Set details of the order object again here
                    tvOrderId.text = o.id
                    tvUserID.text = o.userId
                    labelUserID.setOnClickListener {
                        copyToClipboard(o.userId)
                    }
                    tvUserID.setOnClickListener {
                        copyToClipboard(o.userId)
                    }

                    tvUsername.text = o.deliveryInformation.name

                    tvTotalCost.text = getString(
                        R.string.placeholder_price,
                        o.totalCost
                    )

                    tvSuggestedSf.text = getString(
                        R.string.placeholder_price,
                        o.shippingFee
                    )

                    tvGrandTotal.text = getString(
                        R.string.placeholder_price,
                        o.totalPaymentWithShippingFee
                    )

                    val completeAddress = "${o.deliveryInformation.streetNumber} " +
                        "${o.deliveryInformation.city}, " +
                        "${o.deliveryInformation.province}, " +
                        "${o.deliveryInformation.region}, " +
                        o.deliveryInformation.postalCode
                    tvDeliveryAddress.text = completeAddress

                    tvStatus.text = o.status
                    tvCourierType.text = o.courierType
                    tvTrackingNumber.text = o.trackingNumber
                    tvNumberOfItems.text = o.orderDetailList.count().toString()

                    val paymentMethodStr = when (o.paymentMethod) {
                        PaymentMethod.COD.name -> {
                            "Cash On Delivery"
                        }
                        PaymentMethod.CREDIT_DEBIT.name -> {
                            "Credit/Debit Card / Paypal"
                        }
                        else -> ""
                    }

                    tvPaymentMethod.text = paymentMethodStr
                    tvPaid.text = if (o.paid) "Yes" else "No"

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_detail_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_copy_to_clipboard -> {
                viewModel.order.observe(viewLifecycleOwner) {
                    copyToClipboard(it.id)
                }
                true
            }
            else -> false
        }
    }

    private fun copyToClipboard(text: String) {
        val clipBoard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val clip = ClipData.newPlainText(text, text)

        clipBoard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Copied to clipboard",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onReturnItemClicked(item: OrderDetail) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            loadingDialog.show()
        }

        val canReturnItem = viewModel.checkItemIfCanReturn(item)

        withContext(Dispatchers.Main) {
            loadingDialog.dismiss()

            if (canReturnItem) {
                val action =
                    OrderDetailListFragmentDirections.actionOrderDetailListFragmentToRequestReturnItemFragment(
                        item,
                        item.id,
                        false
                    )
                findNavController().navigate(action)
            } else {
                Snackbar.make(
                    requireView(),
                    "You already request to return ${item.product.name} (${item.size})",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onAddReviewClicked(item: OrderDetail) = CoroutineScope(Dispatchers.IO).launch {
        withContext(Dispatchers.Main) {
            loadingDialog.show()
        }

        val canAddReview = viewModel.checkItemIfCanAddReview(item)

        withContext(Dispatchers.Main) {
            loadingDialog.dismiss()

            if (canAddReview) {
                viewModel.userInfo.value?.let { userInfo ->
                    setFragmentResultListener(ADD_REVIEW_REQUEST) { _, bundle ->
                        val result = bundle.getBoolean(ADD_REVIEW_RESULT)
                        if (result) {
                            Snackbar.make(
                                requireView(),
                                "Review submitted successfully.",
                                Snackbar.LENGTH_SHORT
                            ).show()

                            viewModel.fetchOrderWithDetailsById(args.orderId, userInfo.userId)
                        }
                    }

                    val action = OrderDetailListFragmentDirections
                        .actionOrderDetailListFragmentToAddReviewFragment(
                            item,
                            userInfo,
                            item.product.name
                        )
                    findNavController().navigate(action)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    "Review for item ${item.product.name} (${item.size}) is submitted.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }
}
