package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Checkout
import com.teampym.onlineclothingshopapplication.data.room.Cart
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_cart.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import java.math.BigDecimal

private const val TAG = "CartFragment"

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartAdapter.OnItemCartListener {

    private lateinit var binding: FragmentCartBinding

    private lateinit var adapter: CartAdapter

    private val viewModel: CartViewModel by viewModels()

    private var userId = ""

    private var cartList: List<Cart> = emptyList()

    private var total: BigDecimal = 0.toBigDecimal()

    private lateinit var loadingDialog: LoadingDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        loadingDialog = LoadingDialog(requireActivity())
        adapter = CartAdapter(this)

        val currentUser = getFirebaseUser()
        if (currentUser != null) {
            userId = currentUser.uid
        } else {
            Toast.makeText(
                requireContext(),
                "Please log in first to view your cart.",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigate(R.id.action_global_categoryFragment)
        }

        binding.apply {
            btnCheckOut.setOnClickListener {
                val outOfStockList = cartList.filter { it.inventory.stock == 0L }

                if (outOfStockList.isNotEmpty()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("DELETE OUT OF STOCK ITEMS")
                        .setMessage("Are you sure you want to proceed? You cannot reverse this action.")
                        .setPositiveButton("YES") { _, _ ->
                            loadingDialog.show()

                            viewModel.onDeleteOutOfStockItems(outOfStockList)
                            viewModel.onCartUpdated(userId, adapter.currentList)
                        }.setNegativeButton("NO") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    loadingDialog.show()

                    viewModel.onCartUpdated(userId, adapter.currentList)
                }
            }

            recyclerViewCart.setHasFixedSize(true)
            recyclerViewCart.adapter = adapter
        }

        viewModel.cart.observe(viewLifecycleOwner) { cart ->
            if (loadingDialog.isActive()) {
                loadingDialog.dismiss()
            }

            adapter.submitList(cart)
            btnCheckOut.isEnabled = cart.isNotEmpty()

            cartList = cart

            if (cart.isEmpty()) {
                binding.recyclerViewCart.visibility = View.INVISIBLE
                binding.labelNoCartItem.visibility = View.VISIBLE
            }

            total = cart.sumOf { it.calculatedTotalPrice }
            val totalText = "$" + String.format("%.2f", total)
            binding.tvMerchandiseTotal.text = totalText
        }

        lifecycleScope.launchWhenStarted {
            viewModel.cartEvent.collectLatest { event ->
                when (event) {
                    is CartViewModel.CartEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    CartViewModel.CartEvent.NavigateToCheckOutFragment -> {
                        loadingDialog.dismiss()

                        val action = CartFragmentDirections.actionCartFragmentToCheckOutFragment(
                            cart = Checkout(userId, cartList, total)
                        )
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser

    @ExperimentalCoroutinesApi
    override fun onAddQuantity(cartId: String, pos: Int) {
        viewModel.onQuantityUpdated(cartId, CartFlag.ADDING.toString())
        adapter.notifyItemChanged(pos)
    }

    @ExperimentalCoroutinesApi
    override fun onRemoveQuantity(cartId: String, pos: Int) {
        viewModel.onQuantityUpdated(cartId, CartFlag.REMOVING.toString())
        adapter.notifyItemChanged(pos)
    }

    override fun onDeleteItemClicked(cartId: String, pos: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("DELETE ITEM")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("YES") { _, _ ->
                viewModel.onDeleteItemSelected(userId, cartId)
                adapter.notifyItemRemoved(pos)
            }.setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onFailure(msg: String) {
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
    }
}
