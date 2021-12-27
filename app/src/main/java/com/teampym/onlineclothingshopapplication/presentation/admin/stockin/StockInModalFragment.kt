package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.databinding.FragmentStockInModalBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StockInModalFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentStockInModalBinding

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

        val product = args.product

        binding.apply {
            if (product.inventoryList.isNotEmpty()) {
                for (inventory in product.inventoryList) {
                    val chip = layoutInflater.inflate(R.layout.inventory_item, null, false) as Chip
                    chip.id = View.generateViewId()
                    chip.text = inventory.size
                    chip.isCheckable = true
                    chip.isEnabled = inventory.stock > 0
                    chip.setOnClickListener {
                        selectedInv = inventory
                        binding.tvAvailableStocks.text =
                            getString(R.string.label_add_stock_available, selectedInv!!.stock)
                    }
                    binding.chipSizeGroup.addView(chip)
                }
                binding.chipSizeGroup.isSingleSelection = true
            }

            btnSubmit.setOnClickListener {
                if (selectedInv != null) {
                    val stockToAdd = etAddStock.text.toString().toInt()
                    viewModel.onSubmitClicked(
                        product.productId,
                        selectedInv!!.inventoryId,
                        stockToAdd
                    )
                    Toast.makeText(
                        requireContext(),
                        "Adding stock...",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
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
