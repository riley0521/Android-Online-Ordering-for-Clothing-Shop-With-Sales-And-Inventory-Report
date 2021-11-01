package com.teampym.onlineclothingshopapplication.presentation.client.cart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.CartFlag
import com.teampym.onlineclothingshopapplication.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CartFragment : Fragment(R.layout.fragment_cart), CartAdapter.OnItemCartListener {

    private lateinit var binding: FragmentCartBinding

    private val viewModel: CartViewModel by viewModels()

    private lateinit var adapter: CartAdapter

    private var userInfo: UserInformation? = null

    @Inject
    lateinit var userInformationDao: UserInformationDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCartBinding.bind(view)

        val user = FirebaseAuth.getInstance().currentUser

        adapter = CartAdapter(this)

        lifecycleScope.launchWhenStarted {

            if(user != null) {
                userInfo = userInformationDao.getCurrentUser(user.uid)
                viewModel.getCart(user.uid)
            } else {
                Toast.makeText(requireContext(), "Please log in first to view your cart.", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_cartFragment_to_categoryFragment)
            }

            viewModel.cart.observe(viewLifecycleOwner) {
                if (it != null) {
                    adapter.submitList(it)

                    // TODO("Display totalOfCart")
                } else {
                    binding.tvMerchandiseTotal.text = "$${userInfo!!.totalOfCart.toBigDecimal()}"
                    binding.recyclerViewCart.visibility = View.INVISIBLE
                    binding.labelNoCartItem.visibility = View.VISIBLE
                }
            }
        }

        binding.apply {
            recyclerViewCart.setHasFixedSize(true)
            recyclerViewCart.adapter = adapter
        }
    }

    override fun onAddQuantity(cartId: String) {
        viewModel.updateCartItemQty(userInfo!!.userId, cartId, CartFlag.ADDING.toString())
    }

    override fun onRemoveQuantity(cartId: String) {
        viewModel.updateCartItemQty(userInfo!!.userId, cartId, CartFlag.REMOVING.toString())
    }

    override fun onFailure(msg: String) {
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()
    }
}