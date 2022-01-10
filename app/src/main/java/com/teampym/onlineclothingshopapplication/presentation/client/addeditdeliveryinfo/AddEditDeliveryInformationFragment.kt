package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
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

    private lateinit var newDeliveryInformation: DeliveryInformation

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

                saveStateOfDeliveryInfo(it)
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
            edtRegion.setOnClickListener {
                val action =
                    AddEditDeliveryInformationFragmentDirections.actionAddEditDeliveryInformationFragmentToSelectRegionProvinceCityFragment(
                        0,
                        0,
                        "Select Region"
                    )
                findNavController().navigate(action)
            }

            edtFullName.setText(viewModel.fullName)
            edtPhoneNo.setText(viewModel.phoneNumber)
            edtPostalCode.setText(viewModel.postalCode)
            edtDetailedAddress.setText(viewModel.streetNumber)

            edtFullName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.fullName = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            edtPhoneNo.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.phoneNumber = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            edtPostalCode.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.postalCode = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            edtDetailedAddress.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.streetNumber = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                when {
                    isNewFormValid() -> {
                        newDeliveryInformation = DeliveryInformation(
                            name = edtFullName.text.toString().trim(),
                            contactNo = edtPhoneNo.text.toString().trim(),
                            region = selectedRegion,
                            province = selectedProvince,
                            city = selectedCity,
                            streetNumber = edtDetailedAddress.text.toString().trim(),
                            postalCode = edtPostalCode.text.toString().trim(),
                            userId = userId,
                            isPrimary = switchDefaultAddress.isChecked
                        )

                        viewModel.onSubmitClicked(newDeliveryInformation, false)
                    }
                    isEditFormValid() -> {
                        editDeliveryInformation?.name = edtFullName.text.toString()
                        editDeliveryInformation?.contactNo = edtPhoneNo.text.toString()
                        editDeliveryInformation?.region = selectedRegion
                        editDeliveryInformation?.province = selectedProvince
                        editDeliveryInformation?.city = selectedCity
                        editDeliveryInformation?.streetNumber = edtDetailedAddress.text.toString()
                        editDeliveryInformation?.postalCode = edtPostalCode.text.toString()
                        editDeliveryInformation?.userId = userId
                        editDeliveryInformation?.isPrimary = switchDefaultAddress.isChecked
                        editDeliveryInformation?.let { edited ->
                            viewModel.onSubmitClicked(
                                edited,
                                true
                            )
                        }
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
            if (it.name.isNotBlank()) {
                selectedRegion = it.name
                binding.edtRegion.text = it.name
                editDeliveryInformation?.region = it.name
            }
        }

        viewModel.selectedProvince.observe(viewLifecycleOwner) {
            if (it.name.isNotBlank()) {
                selectedProvince = it.name
                binding.edtProvince.text = it.name
                editDeliveryInformation?.province = it.name
            }
        }

        viewModel.selectedCity.observe(viewLifecycleOwner) {
            if (it.name.isNotBlank()) {
                selectedCity = it.name
                binding.edtCity.text = it.name
                editDeliveryInformation?.city = it.name
            }
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

    private fun saveStateOfDeliveryInfo(deliveryInfo: DeliveryInformation) {
        viewModel.fullName = deliveryInfo.name
        viewModel.streetNumber = deliveryInfo.streetNumber
        viewModel.phoneNumber = deliveryInfo.contactNo
        viewModel.postalCode = deliveryInfo.postalCode
    }

    private fun showAlertDialog(msg: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("WARNING")
            .setMessage(msg)
            .setNegativeButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun isNewFormValid(): Boolean {
        binding.apply {
            return edtFullName.text.isNotBlank() &&
                edtPhoneNo.text.isNotBlank() &&
                edtPostalCode.text.isNotBlank() &&
                edtDetailedAddress.text.isNotBlank() &&
                selectedRegion.isNotBlank() &&
                selectedProvince.isNotBlank() &&
                selectedCity.isNotBlank() &&
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
