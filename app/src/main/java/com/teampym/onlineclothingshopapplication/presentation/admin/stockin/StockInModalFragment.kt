package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentStockInModalBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

const val STOCK_IN_REQUEST = "stock_in_request"
const val STOCK_IN_RESULT = "stock_in_result"

@AndroidEntryPoint
class StockInModalFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentStockInModalBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<StockInViewModel>()

    private val args by navArgs<StockInModalFragmentArgs>()

    private var selectedInv: Inventory? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stock_in_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentStockInModalBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        viewModel.fetchAllSizes(args.product)

        viewModel.product.observe(viewLifecycleOwner) {
            it?.let { product ->
                setupViews(product)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.stockInEvent.collectLatest { event ->
                when (event) {
                    is StockInViewModel.StockInEvent.NavigateBackWithResult -> {
                        loadingDialog.dismiss()
                        setFragmentResult(
                            STOCK_IN_REQUEST,
                            bundleOf(STOCK_IN_RESULT to event.isSuccess)
                        )
                        findNavController().popBackStack()
                    }
                    is StockInViewModel.StockInEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupViews(product: Product) {
        binding.apply {
            if (product.inventoryList.isNotEmpty()) {
                for (inventory in product.inventoryList) {
                    val chip = layoutInflater.inflate(R.layout.inventory_item, null, false) as Chip
                    chip.id = View.generateViewId()
                    chip.text = inventory.size
                    chip.isCheckable = true
                    chip.setOnClickListener {
                        binding.tvAvailableStocks.text = getString(
                            R.string.placeholder_add_stock_available,
                            inventory.stock
                        )
                        selectedInv = inventory
                    }
                    binding.chipSizeGroup.addView(chip)
                }
                binding.chipSizeGroup.isSingleSelection = true
            }

            btnSubmit.setOnClickListener {
                if (selectedInv != null) {
                    etAddStock.text.toString().let { stockToAdd ->
                        if (stockToAdd.isNotBlank()) {
                            loadingDialog.show()

                            viewModel.onSubmitClicked(
                                product.productId,
                                selectedInv!!.inventoryId,
                                stockToAdd.toInt()
                            )
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Please enter the number of stocks you want to add",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please select a size where you want to add stock",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
