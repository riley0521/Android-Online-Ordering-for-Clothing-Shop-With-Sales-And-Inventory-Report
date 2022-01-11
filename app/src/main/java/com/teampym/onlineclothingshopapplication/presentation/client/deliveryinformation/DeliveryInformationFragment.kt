package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.databinding.FragmentDeliveryInformationBinding
import com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo.ADD_EDIT_DELETE_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo.ADD_EDIT_DELETE_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DeliveryInformationFragment :
    Fragment(R.layout.fragment_delivery_information),
    DeliveryInformationAdapter.OnDeliveryInformationListener {

    private lateinit var binding: FragmentDeliveryInformationBinding

    private val viewModel by viewModels<DeliveryInformationViewModel>()

    private lateinit var adapter: DeliveryInformationAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDeliveryInformationBinding.bind(view)

        adapter = DeliveryInformationAdapter(this)

        viewModel.deliveryInformation.observe(viewLifecycleOwner) { deliveryInfoList ->
            if (deliveryInfoList.isNotEmpty()) {
                val sortedList = deliveryInfoList.sortedByDescending { it.isPrimary }
                adapter.submitList(sortedList)
                binding.recyclerDeliveryInformation.setHasFixedSize(true)
                binding.recyclerDeliveryInformation.visibility = View.VISIBLE
                binding.recyclerDeliveryInformation.adapter = adapter
                binding.tvNoAddressYet.visibility = View.GONE
            }
        }

        binding.apply {
            fabCreateNew.setOnClickListener {
                val action =
                    DeliveryInformationFragmentDirections.actionDeliveryInformationFragmentToAddEditDeliveryInformationFragment(
                        null,
                        "Add New Address"
                    )
                findNavController().navigate(action)
            }
        }

        setFragmentResultListener(ADD_EDIT_DELETE_REQUEST) { _, bundle ->
            val result = bundle.getInt(ADD_EDIT_DELETE_RESULT)
            viewModel.onAddEditOrDeleteResult(result)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.deliveryInformationEvent.collectLatest { event ->
                when (event) {
                    is DeliveryInformationViewModel.DeliveryInfoEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onItemClicked(deliveryInfo: DeliveryInformation) {
        val action = DeliveryInformationFragmentDirections
            .actionDeliveryInformationFragmentToAddEditDeliveryInformationFragment(
                deliveryInfo,
                "Edit Address"
            )
        findNavController().navigate(action)
    }
}
