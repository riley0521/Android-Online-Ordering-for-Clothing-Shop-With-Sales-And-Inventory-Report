package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.databinding.FragmentInventoryModalBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InventoryModalFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentInventoryModalBinding

    private val viewModel: InventoryViewModel by viewModels()

    private val args by navArgs<InventoryModalFragmentArgs>()

    private lateinit var product: Product

    private var selectedInv: Inventory? = null

    private var count = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inventory_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentInventoryModalBinding.bind(view)

        val currentUser = FirebaseAuth.getInstance().currentUser
        var userId = ""
        if (currentUser != null) {
            userId = currentUser.uid
        }

        product = args.product

        binding.apply {
            Glide.with(requireView())
                .load(product.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(imgProductInventory)

            tvProductName.text = product.name

            btnViewSizeChart.setOnClickListener {
                findNavController().navigate(R.id.action_inventoryModalFragment_to_sizeChartFragment)
            }

            btnAddToCart.setOnClickListener {
                viewModel.addToCart(
                    userId,
                    product,
                    selectedInv!!,
                    count
                )
                Toast.makeText(
                    requireContext(),
                    "Added ${product.name} to cart.",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }
        }

        if (product.inventoryList.isNotEmpty()) {
            for (inventory in product.inventoryList) {
                val chip = layoutInflater.inflate(R.layout.inventory_item, null, false) as Chip
                chip.id = View.generateViewId()
                chip.text = inventory.size
                chip.isCheckable = true
                chip.isEnabled = inventory.stock > 0
                chip.setOnClickListener {
                    binding.apply {
                        btnAddToCart.isEnabled = chipSizeGroup
                            .checkedChipIds
                            .count() == 1 && userId.isNotEmpty()

                        val numberOfAvailableStocksForSize = "(Stock: ${inventory.stock})"
                        tvAvailableStocks.text = numberOfAvailableStocksForSize
                        tvAvailableStocks.isVisible = chipSizeGroup.checkedChipIds.count() == 1

                        // Set the inventory object to global variable
                        // When chip is selected
                        selectedInv = inventory

                        count = 1L
                        tvCount.text = count.toString()

                        btnAdd.setOnClickListener {
                            if (count++ > inventory.stock) {
                                Toast.makeText(
                                    requireContext(),
                                    "You reached the maximum number of stocks available.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                tvCount.text = count.toString()
                            }
                        }

                        btnRemove.setOnClickListener {
                            if (count-- < 1) {
                                Toast.makeText(
                                    requireContext(),
                                    "1 is the minimum quantity.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                tvCount.text = count.toString()
                            }
                        }
                    }
                }
                binding.chipSizeGroup.addView(chip)
            }
            binding.chipSizeGroup.isSingleSelection = true
        }
    }
}
