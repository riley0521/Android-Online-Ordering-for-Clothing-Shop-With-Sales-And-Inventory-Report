package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditDeliveryInformationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditDeliveryInformationFragment :
    Fragment(R.layout.fragment_add_edit_delivery_information) {

    private lateinit var binding: FragmentAddEditDeliveryInformationBinding

    private val args by navArgs<AddEditDeliveryInformationFragmentArgs>()

    private val viewModel: DeliveryInfoSharedViewModel by activityViewModels()

    private var regionId = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditDeliveryInformationBinding.bind(view)

        val editDeliveryInfo = args.deliveryInfo

        viewModel.userDeliveryInfoList.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.switchDefaultAddress.isChecked = true
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
                // TODO("DELETE THE ADDRESS THE USER IS EDITING")
            }

            btnSubmit.setOnClickListener {
                // TODO("SAVE THE ADDRESS TO REMOTE AND LOCAL DB")
            }
        }

        lifecycleScope.launch {
            viewModel.selectedRegion.observe(viewLifecycleOwner) {
                binding.edtRegion.text = it.name
            }

            viewModel.selectedProvince.observe(viewLifecycleOwner) {
                binding.edtProvince.text = it.name
            }

            viewModel.selectedCity.observe(viewLifecycleOwner) {
                binding.edtCity.text = it.name
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
        }
    }
}
