package com.teampym.onlineclothingshopapplication.presentation.client.return_items

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentReturnItemsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ReturnItemsFragment :
    Fragment(R.layout.fragment_return_items),
    ReturnItemsAdapter.ReturnItemsListener {

    private lateinit var binding: FragmentReturnItemsBinding

    private lateinit var adapter: ReturnItemsAdapter

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<ReturnItemsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentReturnItemsBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = ReturnItemsAdapter(this, requireContext(), viewModel.userType)

        binding.apply {
            rvReturnItems.setHasFixedSize(true)
            rvReturnItems.layoutManager = LinearLayoutManager(requireContext())
            rvReturnItems.adapter = adapter
        }

        lifecycleScope.launchWhenResumed {
            viewModel.orderItems.collectLatest {
                adapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.returnItemsEvent.collectLatest { event ->
                when (event) {
                    is ReturnItemsViewModel.ReturnItemsEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is ReturnItemsViewModel.ReturnItemsEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.refresh()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.return_items_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.by_requested -> {
                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    "REQUESTED TO RETURN ITEM/S"

                viewModel.sort.postValue(ReturnItemsSort.BY_REQUESTED)
                true
            }
            R.id.by_returned -> {
                (requireActivity() as AppCompatActivity).supportActionBar?.title = "RETURNED ITEM/S"

                viewModel.sort.postValue(ReturnItemsSort.BY_RETURNED)
                true
            }
            else -> false
        }
    }

    override fun onViewReasonClicked(item: OrderDetail) {
        val action =
            ReturnItemsFragmentDirections.actionReturnItemsFragmentToRequestReturnItemFragment(
                item,
                item.id,
                true
            )
        findNavController().navigate(action)
    }

    override fun onConfirmReturnItemClicked(item: OrderDetail) {
        AlertDialog.Builder(requireContext())
            .setTitle("CONFIRM RETURN ITEM")
            .setMessage("Are you sure that this item is returned successfully?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.confirmOrder(item)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
