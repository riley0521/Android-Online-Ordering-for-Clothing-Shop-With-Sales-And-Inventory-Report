package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.DeliveryInformation
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditDeliveryInformationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditDeliveryInformationFragment :
    Fragment(R.layout.fragment_add_edit_delivery_information) {

    private lateinit var binding: FragmentAddEditDeliveryInformationBinding

    private val args by navArgs<AddEditDeliveryInformationFragmentArgs>()

    private val viewModel: DeliveryInfoSharedViewModel by activityViewModels()

    private var userId = ""

    private var regionId = 0L

    private var newDeliveryInformation = DeliveryInformation()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditDeliveryInformationBinding.bind(view)

        val editDeliveryInfo = args.deliveryInfo

        viewModel.userDeliveryInfoList.observe(viewLifecycleOwner) { deliveryInfoList ->
            if (deliveryInfoList.isEmpty()) {
                binding.switchDefaultAddress.isChecked = true
                binding.switchDefaultAddress.setOnCheckedChangeListener { button, _ ->
                    button.isChecked = true
                    Toast.makeText(
                        requireContext(),
                        "You cannot turn this off since this will be your first address.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val default = deliveryInfoList.firstOrNull { it.isPrimary }
                default?.let {
                    binding.switchDefaultAddress.isChecked = it.isPrimary
                }
            }
        }

        binding.apply {
            tvDeleteAddress.isVisible = editDeliveryInfo != null

            edtRegion.setOnClickListener {
                val action =
                    AddEditDeliveryInformationFragmentDirections.actionAddEditDeliveryInformationFragmentToSelectRegionProvinceCityFragment(
                        0,
                        0,
                        "Select Region"
                    )
                findNavController().navigate(action)
            }

            tvDeleteAddress.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("DELETE ADDRESS")
                    .setMessage("Are you sure you want to delete this address? You cannot undo this action.")
                    .setPositiveButton("YES") { _, _ ->
                        if (editDeliveryInfo != null) {
                            viewModel.onDeleteAddressClicked(editDeliveryInfo)
                            findNavController().popBackStack()
                        }
                    }.setNegativeButton("NO") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }

            btnSubmit.setOnClickListener {
                if (isFormValid()) {
                    newDeliveryInformation.name = edtFullName.text.toString()
                    newDeliveryInformation.contactNo = edtPhoneNo.text.toString()
                    newDeliveryInformation.streetNumber = edtDetailedAddress.text.toString()
                    newDeliveryInformation.postalCode = edtPostalCode.text.toString()
                    newDeliveryInformation.userId = userId
                    newDeliveryInformation.isPrimary = switchDefaultAddress.isChecked
                    viewModel.onSubmitClicked(newDeliveryInformation)
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.userId.observe(viewLifecycleOwner) {
            userId = it
        }

        viewModel.selectedRegion.observe(viewLifecycleOwner) {
            binding.edtRegion.text = it.name
            newDeliveryInformation.region = it.name
        }

        viewModel.selectedProvince.observe(viewLifecycleOwner) {
            binding.edtProvince.text = it.name
            newDeliveryInformation.province = it.name
        }

        viewModel.selectedCity.observe(viewLifecycleOwner) {
            binding.edtCity.text = it.name
            newDeliveryInformation.city = it.name
        }

        viewModel.regionId.observe(viewLifecycleOwner) { parentId ->
            if (parentId > 0L) {
                regionId = parentId

                binding.edtProvince.setOnClickListener {
                    val action =
                        AddEditDeliveryInformationFragmentDirections.actionAddEditDeliveryInformationFragmentToSelectRegionProvinceCityFragment(
                            parentId,
                            0,
                            "Select Province"
                        )
                    findNavController().navigate(action)
                }
            } else {
                binding.edtProvince.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Please select a region first.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.provinceId.observe(viewLifecycleOwner) { provinceId ->
            if (regionId > 0L && provinceId > 0L) {
                binding.edtCity.setOnClickListener {
                    val action =
                        AddEditDeliveryInformationFragmentDirections.actionAddEditDeliveryInformationFragmentToSelectRegionProvinceCityFragment(
                            regionId,
                            provinceId,
                            "Select City"
                        )
                    findNavController().navigate(action)
                }
            } else {
                binding.edtCity.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        "Please select a province first.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.event.collectLatest {
                when (it) {
                    is DeliveryInfoSharedViewModel.AddEditDeliveryInformationEvent.ShowMessage -> {
                        Toast.makeText(requireContext(), it.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isFormValid(): Boolean {
        binding.apply {
            return edtFullName.text.isNotBlank() &&
                edtPhoneNo.text.isNotBlank() &&
                edtPostalCode.text.isNotBlank() &&
                edtDetailedAddress.text.isNotBlank() &&
                newDeliveryInformation.region.isNotBlank() &&
                newDeliveryInformation.province.isNotBlank() &&
                newDeliveryInformation.city.isNotBlank() &&
                userId.isNotBlank()
        }
    }
}
