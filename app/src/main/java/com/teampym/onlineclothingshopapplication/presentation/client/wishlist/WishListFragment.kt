package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.databinding.FragmentWishListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WishListFragment : Fragment(R.layout.fragment_wish_list), WishListAdapter.WishListListener {

    private lateinit var binding: FragmentWishListBinding

    private val viewModel by viewModels<WishListViewModel>()

    private lateinit var adapter: WishListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentWishListBinding.bind(view)
        adapter = WishListAdapter(this)

        fetchWishList()
    }

    private fun fetchWishList() {
        FirebaseAuth.getInstance().currentUser?.let {
            lifecycleScope.launchWhenStarted {
                viewModel.getAllWishList(it.uid)
            }
        }

        viewModel.wishList.observe(viewLifecycleOwner) { wishList ->
            if (wishList.isNotEmpty()) {
                adapter.submitList(wishList)

                binding.apply {
                    rvWishList.setHasFixedSize(true)
                    rvWishList.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    rvWishList.adapter = adapter
                }
            } else {
                binding.apply {
                    labelNoItem.isVisible = true
                    rvWishList.isVisible = false
                }
            }
        }
    }

    override fun onResume() {
        fetchWishList()
        super.onResume()
    }

    override fun onItemClicked(item: WishItem) {
        Toast.makeText(
            requireContext(),
            "$item",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onRemoveClicked(item: WishItem, position: Int) {
        Toast.makeText(
            requireContext(),
            "$item [pos: $position]",
            Toast.LENGTH_SHORT
        ).show()
    }
}
