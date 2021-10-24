package com.teampym.onlineclothingshopapplication.presentation.client.inventories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Inventory
import com.teampym.onlineclothingshopapplication.data.models.Product
import com.teampym.onlineclothingshopapplication.databinding.FragmentInventoryModalBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class InventoryModalFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentInventoryModalBinding

    private val viewModel: InventoryViewModel by viewModels()

    private val args by navArgs<InventoryModalFragmentArgs>()

    private lateinit var product: Product

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

        product = args.product

        binding.apply {
            Glide.with(requireView())
                .load(product.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(imgProductInventory)

            tvProductName.text = product.name
        }

        if (product.inventories.size == 1) {
            // TODO("Directly Add to Cart")
            Toast.makeText(requireContext(), "Added ${product.inventories[0].size} Size to your cart", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
        }

        for (inventory in product.inventories) {
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.inventory_item, null, false) as Chip
            chip.id = View.generateViewId()
            chip.text = inventory.size
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                buttonView.isChecked = isChecked
                checkIfValid(inventory)
            }

            binding.chipSizeGroup.addView(chip)
        }

        binding.chipSizeGroup.isSingleSelection = true

        lifecycleScope.launchWhenStarted {
            viewModel.productEvent.collect { event ->
                when (event) {
                    is InventoryViewModel.ProductEvent.AddOrUpdateCart -> {
                        Toast.makeText(
                            requireContext(),
                            "Added ${event.name} (${event.size}) to cart. Qty: ${event.count}",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun checkIfValid(inventory: Inventory) {
        binding.btnAddToCart.isEnabled = binding.chipSizeGroup.checkedChipIds.count() == 1

        binding.btnAddToCart.setOnClickListener {
            viewModel.addToCart(
                product,
                inventory
            )
        }
    }
}