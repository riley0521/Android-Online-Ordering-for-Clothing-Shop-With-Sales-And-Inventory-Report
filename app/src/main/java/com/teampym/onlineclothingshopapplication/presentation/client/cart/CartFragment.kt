package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Utils
import com.teampym.onlineclothingshopapplication.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartAdapter.OnItemCartListener {

    private lateinit var binding: FragmentCartBinding

    private val viewModel: CartViewModel by viewModels()

    private lateinit var adapter: CartAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        val user = FirebaseAuth.getInstance().currentUser
        adapter = CartAdapter(this)

        if(user != null) {
            viewModel.getCart(user.uid)
        } else {
            Toast.makeText(requireContext(), "Please log in first to view your cart.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_cartFragment_to_categoryFragment)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.cartFlow.collect {
                if (it.isNotEmpty()) {
                    adapter.submitList(it)

                    binding.tvMerchandiseTotal.text = "$" + "%.2f".format(Utils.currentUser!!.totalOfCart)
                } else {
                    binding.recyclerViewCart.isVisible = false
                    binding.tvNoItems.isVisible = true
                }
            }
        }

        binding.apply {
            recyclerViewCart.setHasFixedSize(true)
            recyclerViewCart.adapter = adapter
        }

    }

    override fun onAddMinusQuantity(cart: Cart, qty: Long) {
        // Add Item's quantity in the cart
        viewModel.updateCartItemQty(cart, qty)
    }

    override fun onFailure(msg: String) {
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
    }
}