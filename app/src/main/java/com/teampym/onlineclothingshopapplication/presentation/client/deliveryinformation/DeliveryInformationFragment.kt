package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
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
                val filteredList = deliveryInfoList.filter { !it.isPrimary }
                adapter.submitList(filteredList)
                binding.recyclerDeliveryInformation.setHasFixedSize(true)
                binding.recyclerDeliveryInformation.visibility = View.VISIBLE
                binding.recyclerDeliveryInformation.adapter = adapter

                binding.tvDeliveryAddressTitle.isVisible = deliveryInfoList.size > 1
                binding.tvNoAddressYet.visibility = View.GONE

                deliveryInfoList.firstOrNull { it.isPrimary }?.let { info ->
                    binding.apply {
                        viewSelectedDeliveryInfo.visibility = View.VISIBLE

                        val contact = if (info.contactNo[0].toString() == "0")
                            info.contactNo.length.let {
                                info.contactNo.substring(
                                    1,
                                    it
                                )
                            } else info.contactNo

                        val nameAndContact = "${info.name} | (+63) $contact"
                        tvNameAndContact.text = nameAndContact

                        val completeAddress = "${info.streetNumber} " +
                            "${info.city}, " +
                            "${info.province}, " +
                            "${info.province}, " +
                            info.postalCode
                        tvAddress.text = completeAddress

                        btnDelete.setOnClickListener {
                            showDeleteDeliveryInformationDialog(info)
                        }

                        btnEdit.setOnClickListener {
                            val action =
                                DeliveryInformationFragmentDirections.actionDeliveryInformationFragmentToAddEditDeliveryInformationFragment(
                                    info,
                                    "Edit Address"
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
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

    private fun showDeleteDeliveryInformationDialog(info: DeliveryInformation) {
        AlertDialog.Builder(requireContext())
            .setTitle("DELETE DELIVERY INFORMATION")
            .setMessage(
                "Are you sure you want to delete this delivery information?\n" +
                    "Reminder: You cannot reverse this action"
            )
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.onDeleteClicked(info)
                dialog.dismiss()
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onDeleteClicked(deliveryInfo: DeliveryInformation) {
        showDeleteDeliveryInformationDialog(deliveryInfo)
    }

    override fun onMakeDefaultClicked(deliveryInfo: DeliveryInformation) {
        AlertDialog.Builder(requireContext())
            .setTitle("Set Default Delivery Information")
            .setMessage(
                "Are you sure you want to make this address your default delivery address?\n" +
                    "Reminder: You cannot reverse this action"
            )
            .setPositiveButton("Yes") { dialog, _ ->
                viewModel.onDeliveryInformationDefaultChanged(
                    deliveryInfo.copy(isPrimary = true)
                )
                dialog.dismiss()
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onEditClicked(deliveryInfo: DeliveryInformation) {
        val action =
            DeliveryInformationFragmentDirections.actionDeliveryInformationFragmentToAddEditDeliveryInformationFragment(
                deliveryInfo,
                "Edit Address"
            )
        findNavController().navigate(action)
    }
}
