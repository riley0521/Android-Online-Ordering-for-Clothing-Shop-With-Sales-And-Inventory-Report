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
import androidx.paging.PagingData
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Order
import com.teampym.onlineclothingshopapplication.data.util.CANCELLED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.COMPLETED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.DELIVERY_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.RETURNED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.SHIPPED_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.SHIPPING_ORDERS
import com.teampym.onlineclothingshopapplication.data.util.Status
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderListBinding
import com.teampym.onlineclothingshopapplication.presentation.client.others.AGREE_TO_SF_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.others.AGREE_TO_SF_RESULT
import com.teampym.onlineclothingshopapplication.presentation.client.others.CANCEL_REASON_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.others.CANCEL_REASON_RESULT
import com.teampym.onlineclothingshopapplication.presentation.client.others.SHIPPING_FEE_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.others.SHIPPING_FEE_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.util.* // ktlint-disable no-wildcard-imports

@AndroidEntryPoint
class OrderListFragment : Fragment(R.layout.fragment_order_list), OrderListAdapter.OnOrderListener {

    private lateinit var binding: FragmentOrderListBinding

    private val args by navArgs<OrderListFragmentArgs>()

    private val viewModel by viewModels<OrderListViewModel>()

    private lateinit var searchView: SearchView

    private lateinit var adapter: OrderListAdapter

    private var currentPagingData: PagingData<Order>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderListBinding.bind(view)

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
                    is OrderListViewModel.OrderListEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()

                        // Set the RefreshLayout to true
                        // And refresh the adapter to fetch fresh data.
                        binding.refreshLayout.isRefreshing = true
                        adapter.refresh()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private fun collectOrderPagingData() {
        viewModel.orders.observe(viewLifecycleOwner) {
            currentPagingData = it
            adapter.submitData(viewLifecycleOwner.lifecycle, it)

            binding.refreshLayout.isRefreshing = false
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
            R.id.action_returned -> {
                viewModel.statusQuery.value = Status.RETURNED.name
                (requireActivity() as AppCompatActivity).supportActionBar?.title = RETURNED_ORDERS
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

    override fun onSuggestClicked(item: Order) {
        showSuggestShipFeeModalForAdmin(item)
    }

    override fun onAgreeToSfClicked(item: Order) {
        setFragmentResultListener(AGREE_TO_SF_REQUEST) { _, bundle ->
            val result = bundle.getString(AGREE_TO_SF_RESULT)
            if (currentPagingData != null) {
                viewModel.onAgreeToSfResult(
                    result ?: "",
                    item,
                    currentPagingData!!,
                    OrderRemoveEvent.Remove(item)
                )
            }
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToAgreeToShippingFeeDialogFragment(
                item
            )
        findNavController().navigate(action)
    }

    private fun showSuggestShipFeeModalForAdmin(item: Order) {
        setFragmentResultListener(SHIPPING_FEE_REQUEST) { _, bundle ->
            val result = bundle.getString(SHIPPING_FEE_RESULT)
            if (currentPagingData != null) {
                viewModel.onSuggestedShippingFeeResult(
                    result ?: "",
                    item,
                    currentPagingData!!,
                    OrderRemoveEvent.Remove(item)
                )
            }
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToShippingFeeDialogFragment(item)
        findNavController().navigate(action)
    }

    private fun showCancelModalForAdmin(item: Order) {
        setFragmentResultListener(CANCEL_REASON_REQUEST) { _, bundle ->
            val result = bundle.getString(CANCEL_REASON_RESULT)
            if (currentPagingData != null) {
                viewModel.onAdminCancelResult(
                    result ?: "",
                    item,
                    currentPagingData!!,
                    OrderRemoveEvent.Remove(item)
                )
            }
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
                if (currentPagingData != null) {
                    viewModel.cancelOrder(
                        item,
                        currentPagingData!!,
                        OrderRemoveEvent.Remove(item)
                    )
                }
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
    }

    sealed class OrderRemoveEvent {
        data class Remove(val order: Order) : OrderRemoveEvent()
    }
}
