package com.teampym.onlineclothingshopapplication.presentation.client.wishlist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.WishItem
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentWishListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class WishListFragment : Fragment(R.layout.fragment_wish_list), WishListAdapter.WishListListener {

    private lateinit var binding: FragmentWishListBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<WishListViewModel>()

    private lateinit var adapter: WishListAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentWishListBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = WishListAdapter(this, requireContext())

        fetchWishList()

        lifecycleScope.launchWhenStarted {
            viewModel.wishListEvent.collectLatest { event ->
                when (event) {
                    is WishListViewModel.WishListEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is WishListViewModel.WishListEvent.ShowMessageAndNotifyAdapter -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.notifyItemRemoved(event.position)
                    }
                }
            }
        }
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
        val action = WishListFragmentDirections.actionWishListFragmentToProductDetailFragment(
            null,
            item.name,
            item.productId
        )
        findNavController().navigate(action)
    }

    override fun onRemoveClicked(item: WishItem, position: Int) {
        loadingDialog.show()
        viewModel.removeFromWishList(item, position)
    }
}
