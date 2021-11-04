package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Cart
import com.teampym.onlineclothingshopapplication.data.models.Checkout
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import com.teampym.onlineclothingshopapplication.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartAdapter.OnItemCartListener {

    private lateinit var binding: FragmentCartBinding

    private val viewModel: CartViewModel by viewModels()

    private lateinit var adapter: CartAdapter

    private var userId = ""

    private var cartList: List<Cart> = emptyList()

    private var total: BigDecimal = 0.toBigDecimal()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        adapter = CartAdapter(this)

        if (getFirebaseUser() == null) {
            Toast.makeText(
                requireContext(),
                "Please log in first to view your cart.",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigate(R.id.action_cartFragment_to_categoryFragment)
        }

        binding.apply {
            btnCheckOut.setOnClickListener {
                val action = CartFragmentDirections.actionCartFragmentToCheckOutFragment(
                    cart = Checkout(getFirebaseUser()?.uid!!, cartList, total)
                )
                findNavController().navigate(action)
            }

            recyclerViewCart.setHasFixedSize(true)
            recyclerViewCart.adapter = adapter
        }

        viewModel.userInformation.observe(viewLifecycleOwner) {
            if (it != null) {
                userId = it.userId
            }
        }

        viewModel.cart.observe(viewLifecycleOwner) { cart ->
            adapter.submitList(cart)

            cartList = cart

            if(cart.isEmpty()) {
                binding.recyclerViewCart.visibility = View.INVISIBLE
                binding.labelNoCartItem.visibility = View.VISIBLE
            }

            total = cart.sumOf { it.calculatedTotalPrice }
            val totalText = "$%2.f".format(total)
            binding.tvMerchandiseTotal.text = totalText
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser

    override fun onAddQuantity(cartId: String, pos: Int) {
        viewModel.updateQty(cartId, CartFlag.ADDING.toString())
        adapter.notifyItemChanged(pos)
    }

    override fun onRemoveQuantity(cartId: String, pos: Int) {
        viewModel.updateQty(cartId, CartFlag.REMOVING.toString())
        adapter.notifyItemChanged(pos)
    }

    override fun onFailure(msg: String) {
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onStop() {
        viewModel.updateCart(userId, adapter.currentList)
        super.onStop()
    }
}