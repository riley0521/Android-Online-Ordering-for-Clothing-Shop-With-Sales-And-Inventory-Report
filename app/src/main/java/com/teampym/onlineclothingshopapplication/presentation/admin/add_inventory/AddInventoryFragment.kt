package com.teampym.onlineclothingshopapplication.presentation.admin.add_inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddInventoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class AddInventoryFragment : Fragment(R.layout.fragment_add_inventory) {

    private lateinit var binding: FragmentAddInventoryBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<AddInventoryFragmentArgs>()

    private val viewModel by viewModels<AddInventoryViewModel>()

    private var availableSizeList: List<String> = listOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddInventoryBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val productId = args.productId
        viewModel.productName = args.productName

        if (productId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Please select a product first " +
                    "before adding a new inventory/size.",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().popBackStack()
        } else {
            viewModel.productId = productId

            loadingDialog.show()
            viewModel.onLoadSizesInitiated()
        }

        viewModel.availableInvList.observe(viewLifecycleOwner) { listOfSizes ->
            loadingDialog.dismiss()

            if (listOfSizes.isNotEmpty()) {
                val sizes = StringBuilder()
                sizes.append("[")
                for (i in listOfSizes.indices) {
                    if (listOfSizes.lastIndex == i) {
                        sizes.append("${listOfSizes[i]}]")
                    } else {
                        sizes.append("${listOfSizes[i]}, ")
                    }
                }

                availableSizeList = listOfSizes.map { it.lowercase().trim() }
                binding.tvAvailableSizes.text = getString(
                    R.string.label_available_sizes,
                    sizes.toString()
                )
                binding.tvAvailableSizes.isVisible = true
            }
        }

        binding.apply {
            etSize.setText(viewModel.inventorySize)
            if (viewModel.inventoryStock > 0) {
                etAvailableStocks.setText(viewModel.inventoryStock.toString())
            }
            if (viewModel.weightInKg > 0.0) {
                etWeightInKg.setText(viewModel.weightInKg.toString())
            }

            etSize.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.inventorySize = s.toString().trim()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            etAvailableStocks.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString().isNotBlank()) {
                        viewModel.inventoryStock = s.toString().toInt()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            etWeightInKg.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s.toString().isNotBlank()) {
                        viewModel.weightInKg = s.toString().toDouble()
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnAddAnotherSize.setOnClickListener {
                if (isSizeExisting()) {
                    Snackbar.make(
                        requireView(),
                        "Size is already existing in database. Avoid duplicates",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    loadingDialog.show()
                    viewModel.onSubmitClicked(true)
                }
            }

            btnSubmit.setOnClickListener {
                if (isSizeExisting()) {
                    Snackbar.make(
                        requireView(),
                        "Size is already existing in database. Avoid duplicates",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    loadingDialog.show()
                    viewModel.onSubmitClicked(false)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.addInventoryEvent.collectLatest { event ->
                when (event) {
                    is AddInventoryViewModel.AddInventoryEvent.NavigateBackWithMessage -> {
                        loadingDialog.dismiss()

                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()
                        try {
                            findNavController().getBackStackEntry(R.id.addEditProductFragment)

                            val categoryId = viewModel.session.first().categoryId
                            val action =
                                AddInventoryFragmentDirections.actionAddInventoryFragmentToProductFragment(
                                    categoryId = categoryId
                                )
                            findNavController().navigate(action)
                        } catch (ex: Exception) {
                            findNavController().popBackStack()
                        }
                    }
                    is AddInventoryViewModel.AddInventoryEvent.ShowSuccessMessageAndResetState -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.onLoadSizesInitiated()

                        resetFields()
                    }
                    is AddInventoryViewModel.AddInventoryEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // We reset the Ui States in ViewModel
    // So, we need to reset the Ui states for the user level as well.
    private fun resetFields() {
        binding.apply {
            etSize.setText(viewModel.inventorySize)
            etAvailableStocks.setText("")
            etWeightInKg.setText("")
        }
    }

    private fun isSizeExisting(): Boolean {
        if (availableSizeList.isNotEmpty()) {
            return availableSizeList.contains(viewModel.inventorySize)
        }
        return false
    }
}
