package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.os.Bundle
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.map
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class ProductFragment : Fragment(R.layout.fragment_product), ProductAdapter.OnProductListener {

    private lateinit var binding: FragmentProductBinding

    private lateinit var adapter: ProductAdapter

    private lateinit var searchView: SearchView

    private val viewModel: ProductViewModel by viewModels()

    private val args by navArgs<ProductFragmentArgs>()

    @Inject
    lateinit var db: FirebaseFirestore

    private lateinit var wishList: UserWithWishList

    private lateinit var loadingDialog: LoadingDialog

    private var myMenu: Menu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductBinding.bind(view)

        loadingDialog = LoadingDialog(requireActivity())

        viewModel.updateCategory(args.categoryId)

        adapter = ProductAdapter(this)

        binding.apply {
            recyclerProducts.setHasFixedSize(true)
            recyclerProducts.layoutManager = GridLayoutManager(requireContext(), 2)
            recyclerProducts.adapter = adapter
        }

        viewModel.userWithWishList.observe(viewLifecycleOwner) { userWithWishList ->
            // Check if the user is customer then hide admin functions when true
            when (userWithWishList.user.userType) {
                UserType.CUSTOMER.name -> {
                    myMenu?.let {
                        it.findItem(R.id.action_add).isVisible = false
                    }
                }
                UserType.ADMIN.name -> {
                    myMenu?.let {
                        it.findItem(R.id.action_add).isVisible = true
                    }
                }
                else -> {
                    myMenu?.let {
                        it.findItem(R.id.action_add).isVisible = false
                        it.findItem(R.id.action_cart).isVisible = false
                    }
                }
            }
            wishList = userWithWishList
        }

        lifecycleScope.launchWhenStarted {
            viewModel.productsFlow.collectLatest {
                val paging = it.map { p ->
                    wishList.wishList.forEach { w ->
                        p.isWishListedByUser = p.productId == w.productId
                    }
                    p
                }
                adapter.submitData(paging)
            }

            viewModel.productEvent.collectLatest { event ->
                when (event) {
                    is ProductViewModel.ProductEvent.ShowMessage -> {
                        Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onItemClicked(product: Product) {
        val action = ProductFragmentDirections.actionProductFragmentToProductDetailFragment(
            product,
            product.name,
            null
        )
        findNavController().navigate(action)
    }

    override fun onShareClicked(product: Product) {
        Toast.makeText(requireContext(), "Sharing ${product.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onAddToCartClicked(product: Product) {
        val action =
            ProductFragmentDirections.actionProductFragmentToInventoryModalFragment(product)
        findNavController().navigate(action)
    }

    override fun onAddToWishListClicked(product: Product) {
        viewModel.addToWishList(wishList.user.userId, product)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_action_menu, menu)

        // After inflating the view, you can not set the reference into a global variable
        myMenu = menu

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Nothing to do here
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Search
                viewModel.searchQuery.value = newText
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_no_sort -> {
                viewModel.updateSortOrder(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_best_seller -> {
                viewModel.updateSortOrder(SortOrder.BY_POPULARITY)
                true
            }
            R.id.action_sort_by_new -> {
                viewModel.updateSortOrder(SortOrder.BY_NEWEST)
                true
            }
            R.id.action_cart -> {
                findNavController().navigate(R.id.action_global_cartFragment)
                true
            }
            R.id.action_new_product -> {
                // TODO("Navigate to add/edit product layout when admin")
                true
            }
            else -> false
        }
    }
}
