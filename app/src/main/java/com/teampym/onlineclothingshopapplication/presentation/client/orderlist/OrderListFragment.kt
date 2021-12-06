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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrderListFragment : Fragment(R.layout.fragment_order_list), OrderListAdapter.OnOrderListener {

    private lateinit var binding: FragmentOrderListBinding

    private val args by navArgs<OrderListFragmentArgs>()

    private val viewModel by viewModels<OrderListViewModel>()

    private lateinit var searchView: SearchView

    private lateinit var adapter: OrderListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderListBinding.bind(view)

        val status = args.status
        viewModel.statusQuery.value = status

        lifecycleScope.launchWhenStarted {
            viewModel.userFlow.collectLatest { user ->
                if (user != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        adapter = OrderListAdapter(
                            user.userType,
                            this@OrderListFragment,
                            requireActivity()
                        )

                        binding.apply {
                            recyclerOrders.setHasFixedSize(true)
                            recyclerOrders.adapter = adapter
                        }
                    }.join()
                    viewModel.ordersFlow.collectLatest {
                        adapter.submitData(it)
                    }
                }
            }

            viewModel.orderEvent.collectLatest { event ->
                when (event) {
                    is OrderListViewModel.OrderListEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adapter.refresh()
                        // adapter.notifyItemRemoved(event.positionToRemove)
                    }
                }
            }
        }

        setHasOptionsMenu(true)
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
        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailListFragment(
            "Order ${item.id}",
            item
        )
        findNavController().navigate(action)
    }

    override fun onCancelClicked(item: Order, userType: String, position: Int) {
        when (userType) {
            UserType.CUSTOMER.name -> {
                showCancelDialogForCustomer(item, position)
            }
            UserType.ADMIN.name -> {
                showCancelModalForAdmin(item, position)
            }
        }
    }

    override fun onSuggestClicked(item: Order, position: Int) {
        showSuggestShipFeeModalForAdmin(item, position)
    }

    override fun onAgreeToSfClicked(item: Order, position: Int) {
        setFragmentResultListener(AGREE_TO_SF_REQUEST) { _, bundle ->
            val result = bundle.getString(AGREE_TO_SF_RESULT)
            viewModel.onAgreeToSfResult(result ?: "", position)
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToAgreeToShippingFeeDialogFragment(
                item
            )
        findNavController().navigate(action)
    }

    private fun showCancelModalForAdmin(item: Order, position: Int) {
        setFragmentResultListener(CANCEL_REASON_REQUEST) { _, bundle ->
            val result = bundle.getString(CANCEL_REASON_RESULT)
            viewModel.onAdminCancelResult(result ?: "", position)
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToCancelReasonDialogFragment(item)
        findNavController().navigate(action)
    }

    private fun showSuggestShipFeeModalForAdmin(item: Order, position: Int) {
        setFragmentResultListener(SHIPPING_FEE_REQUEST) { _, bundle ->
            val result = bundle.getString(SHIPPING_FEE_RESULT)
            viewModel.onSuggestedShippingFeeResult(result ?: "", position)
        }

        val action =
            OrderListFragmentDirections.actionOrderListFragmentToShippingFeeDialogFragment(item)
        findNavController().navigate(action)
    }

    private fun showCancelDialogForCustomer(item: Order, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("CANCEL ORDER")
            .setMessage("Are you sure you want to cancel order?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.cancelOrder(item, position)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
    }
}