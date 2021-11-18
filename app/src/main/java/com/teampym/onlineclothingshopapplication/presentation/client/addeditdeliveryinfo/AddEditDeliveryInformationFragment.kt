package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditDeliveryInformationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val ADD_EDIT_DELETE_REQUEST = "add_edit_delete_request"
const val ADD_EDIT_DELETE_RESULT = "add_edit_delete_result"

@AndroidEntryPoint
class AddEditDeliveryInformationFragment :
    Fragment(R.layout.fragment_add_edit_delivery_information) {

    private lateinit var binding: FragmentAddEditDeliveryInformationBinding

    private val args by navArgs<AddEditDeliveryInformationFragmentArgs>()

    private val viewModel: DeliveryInfoSharedViewModel by activityViewModels()

    private var userId = ""

    private var regionId = 0L

    private var newDeliveryInformation = DeliveryInformation()

    private var editDeliveryInformation: DeliveryInformation? = null

    private var selectedRegion = ""
    private var selectedProvince = ""
    private var selectedCity = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditDeliveryInformationBinding.bind(view)

        editDeliveryInformation = args.deliveryInfo

        editDeliveryInformation?.let {
            binding.apply {
                edtFullName.setText(it.name)
                edtPhoneNo.setText(it.contactNo)

                selectedRegion = it.region
                edtRegion.text = it.region

                selectedProvince = it.province
                edtProvince.text = it.province

                selectedCity = it.city
                edtCity.text = it.city
                edtPostalCode.setText(it.postalCode)
                edtDetailedAddress.setText(it.streetNumber)
                switchDefaultAddress.isChecked = it.isPrimary
            }
        }

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
            }
        }

        binding.apply {
            tvDeleteAddress.isVisible = editDeliveryInformation != null

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
                        if (editDeliveryInformation != null) {
                            viewModel.onDeleteAddressClicked(editDeliveryInformation)
                        }
                    }.setNegativeButton("NO") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }

            btnSubmit.setOnClickListener {
                when {
                    isNewFormValid() -> {
                        newDeliveryInformation.name = edtFullName.text.toString()
                        newDeliveryInformation.contactNo = edtPhoneNo.text.toString()
                        newDeliveryInformation.streetNumber = edtDetailedAddress.text.toString()
                        newDeliveryInformation.postalCode = edtPostalCode.text.toString()
                        newDeliveryInformation.userId = userId
                        newDeliveryInformation.isPrimary = switchDefaultAddress.isChecked
                        viewModel.onSubmitClicked(newDeliveryInformation, false)
                    }
                    isEditFormValid() -> {
                        editDeliveryInformation?.name = edtFullName.text.toString()
                        editDeliveryInformation?.contactNo = edtPhoneNo.text.toString()
                        editDeliveryInformation?.streetNumber = edtDetailedAddress.text.toString()
                        editDeliveryInformation?.postalCode = edtPostalCode.text.toString()
                        editDeliveryInformation?.userId = userId
                        editDeliveryInformation?.isPrimary = switchDefaultAddress.isChecked
                        viewModel.onSubmitClicked(newDeliveryInformation, true)
                    }
                    else -> {
                        when {
                            edtFullName.text.isBlank() -> {
                                showAlertDialog("Set Full Name")
                            }
                            edtPhoneNo.text.isBlank() -> {
                                showAlertDialog("Set Phone Number")
                            }
                            selectedRegion.isBlank() -> {
                                showAlertDialog("Set Region")
                            }
                            selectedProvince.isBlank() -> {
                                showAlertDialog("Set Province")
                            }
                            selectedCity.isBlank() -> {
                                showAlertDialog("Set City")
                            }
                            edtPostalCode.text.isBlank() -> {
                                showAlertDialog("Set Postal Code")
                            }
                            edtDetailedAddress.text.isBlank() -> {
                                showAlertDialog("Set Detailed Address")
                            }
                        }
                    }
                }
            }
        }

        viewModel.userId.observe(viewLifecycleOwner) {
            userId = it
        }

        viewModel.selectedRegion.observe(viewLifecycleOwner) {
            selectedRegion = it.name
            binding.edtRegion.text = it.name
            newDeliveryInformation.region = it.name
            editDeliveryInformation?.region = it.name
        }

        viewModel.selectedProvince.observe(viewLifecycleOwner) {
            selectedProvince = it.name
            binding.edtProvince.text = it.name
            newDeliveryInformation.province = it.name
            editDeliveryInformation?.province = it.name
        }

        viewModel.selectedCity.observe(viewLifecycleOwner) {
            selectedCity = it.name
            binding.edtCity.text = it.name
            newDeliveryInformation.city = it.name
            editDeliveryInformation?.city = it.name
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
                    is DeliveryInfoSharedViewModel.AddEditDeliveryInformationEvent.NavigateBackWithResult -> {
                        setFragmentResult(
                            ADD_EDIT_DELETE_REQUEST,
                            bundleOf(ADD_EDIT_DELETE_RESULT to it.result)
                        )
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun showAlertDialog(msg: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("WARNING")
            .setMessage(msg)
            .show()
    }

    private fun isNewFormValid(): Boolean {
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

    private fun isEditFormValid(): Boolean {
        editDeliveryInformation?.let {
            binding.apply {
                return edtFullName.text.isNotBlank() &&
                    edtPhoneNo.text.isNotBlank() &&
                    edtPostalCode.text.isNotBlank() &&
                    edtDetailedAddress.text.isNotBlank() &&
                    it.region.isNotBlank() &&
                    it.province.isNotBlank() &&
                    it.city.isNotBlank() &&
                    userId.isNotBlank()
            }
        }
        return false
    }
}