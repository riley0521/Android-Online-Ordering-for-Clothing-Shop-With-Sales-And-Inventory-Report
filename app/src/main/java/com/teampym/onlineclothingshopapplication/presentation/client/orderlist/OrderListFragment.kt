package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.util.CANCELLED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.COMPLETED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.CourierType
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.SHIPPED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.SHIPPING_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderListBinding
import com.teampym.onlineclothingshopapplication.presentation.client.others.CANCEL_REASON_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.others.CANCEL_REASON_RESULT
import com.teampym.onlineclothingshopapplication.presentation.client.others.TRACKING_NUMBER_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.others.TRACKING_NUMBER_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.util.* // ktlint-disable no-wildcard-imports

@AndroidEntryPoint
class OrderListFragment : Fragment(R.layout.fragment_order_list), OrderListAdapter.OnOrderListener {

    private lateinit var binding: FragmentOrderListBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<OrderListFragmentArgs>()

    private val viewModel by viewModels<OrderListViewModel>()

    private lateinit var searchView: SearchView

    private lateinit var adapter: OrderListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderListBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val status = args.status
        viewModel.statusQuery.value = status

        viewModel.getOrders()

        viewModel.userSession.observe(viewLifecycleOwner) {
            adapter = OrderListAdapter(
                it.userType,
                this@OrderListFragment,
                requireActivity()
            )
            adapter.withLoadStateHeaderAndFooter(
                header = OrderListLoadStateAdapter(adapter),
                footer = OrderListLoadStateAdapter(adapter)
            )

            binding.apply {
                recyclerOrders.setHasFixedSize(true)
                recyclerOrders.adapter = adapter
            }

            collectOrderPagingData()
        }

        lifecycleScope.launchWhenStarted {
            viewModel.orderEvent.collectLatest { event ->
                when (event) {
                    is OrderListViewModel.OrderListEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adapter.refresh()
                    }
                    is OrderListViewModel.OrderListEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private fun collectOrderPagingData() {
        viewModel.orders.observe(viewLifecycleOwner) {
            binding.refreshLayout.isRefreshing = false
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }

        binding.apply {
            refreshLayout.setOnRefreshListener {
                adapter.refresh()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_action_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Nothing to do here
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Search
                viewModel.searchQuery.value = newText
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_shipping -> {
                viewModel.statusQuery.value = Status.SHIPPING.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = SHIPPING_ORDERS
                true
            }
            R.id.action_shipped -> {
                viewModel.statusQuery.value = Status.SHIPPED.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = SHIPPED_ORDERS
                true
            }
            R.id.action_delivery -> {
                viewModel.statusQuery.value = Status.DELIVERY.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = DELIVERY_ORDERS
                true
            }
            R.id.action_completed -> {
                viewModel.statusQuery.value = Status.COMPLETED.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = COMPLETED_ORDERS
                true
            }
            R.id.action_cancelled -> {
                viewModel.statusQuery.value = Status.CANCELED.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = CANCELLED_ORDERS
                true
            }
            else -> false
        }
    }

    override fun onItemClicked(item: Order) {
        val action =
            OrderListFragmentDirections.actionOrderListFragmentToOrderDetailListFragment(
                title = "Order ${item.id}",
                order = item,
                orderId = item.id
            )
        findNavController().navigate(action)
    }

    override fun onCancelClicked(item: Order, userType: String) {
        val isCancellable = viewModel.checkOrderIfCancellable(item)

        when (userType) {
            UserType.CUSTOMER.name -> {
                if (isCancellable) {
                    showCancelDialogForCustomer(item)
                }
            }
            UserType.ADMIN.name -> {
                if (isCancellable) {
                    showCancelModalForAdmin(item)
                }
            }
        }
    }

    override fun onShipOrderClicked(item: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("SHIP ORDER")
            .setMessage("Are you sure you want to ship this order?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.shipOrder(item)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onReceivedOrderClicked(item: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("MARK ORDER AS RECEIVED")
            .setMessage("Are you sure you want to mark this order as received? You cannot undo this action.")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.receivedOrder(item)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onDeliverOrderClicked(item: Order, type: CourierType) {
        when (type) {
            CourierType.ADMINS -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("DELIVERY ORDER")
                    .setMessage("Are you sure you want to deliver this order?")
                    .setPositiveButton("Yes") { _, _ ->
                        loadingDialog.show()
                        viewModel.deliverOrder(
                            item,
                            type,
                            ""
                        )
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
            CourierType.JNT -> {
                setFragmentResultListener(TRACKING_NUMBER_REQUEST) { _, bundle ->
                    val trackingNumber = bundle.getString(TRACKING_NUMBER_RESULT)

                    loadingDialog.show()
                    viewModel.deliverOrder(
                        item,
                        type,
                        trackingNumber ?: ""
                    )
                }

                findNavController().navigate(R.id.action_orderListFragment_to_trackingNumberDialogFragment)
            }
        }
    }

    override fun onCompleteOrderClicked(item: Order, isSfShoulderedByAdmin: Boolean) {
        loadingDialog.show()
        viewModel.completeOrder(
            item,
            isSfShoulderedByAdmin
        )
    }

    private fun showCancelModalForAdmin(item: Order) {
        setFragmentResultListener(CANCEL_REASON_REQUEST) { _, bundle ->
            val result = bundle.getString(CANCEL_REASON_RESULT)

            loadingDialog.show()
            viewModel.onAdminCancelResult(
                result ?: "",
                item
            )
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToCancelReasonDialogFragment(item)
        findNavController().navigate(action)
    }

    private fun showCancelDialogForCustomer(item: Order) {
        AlertDialog.Builder(requireContext())
            .setTitle("CANCEL ORDER")
            .setMessage("Are you sure you want to cancel order?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.cancelOrder(
                    item
                )
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
