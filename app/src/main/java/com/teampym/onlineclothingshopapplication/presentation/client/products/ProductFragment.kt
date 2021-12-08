package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.os.Bundle
import android.util.Log
import android.view.* // ktlint-disable no-wildcard-imports
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.paging.map
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.room.UserWithWishList
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ProductFragment :
    Fragment(R.layout.fragment_product),
    ProductAdapter.OnProductListener,
    ProductAdminAdapter.OnProductAdapterListener {

    private lateinit var binding: FragmentProductBinding

    private lateinit var adapter: ProductAdapter
    private lateinit var adminAdapter: ProductAdminAdapter

    private lateinit var searchView: SearchView

    private val viewModel: ProductViewModel by viewModels()

    private val args by navArgs<ProductFragmentArgs>()

    @Inject
    lateinit var db: FirebaseFirestore

    private var userAndWishList: UserWithWishList? = null

    private lateinit var loadingDialog: LoadingDialog

    private var addMenu: MenuItem? = null
    private var cartMenu: MenuItem? = null
    private var sortMenu: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = ProductAdapter(this)
        adminAdapter = ProductAdminAdapter(this)

        viewModel.updateCategory(args.categoryId)

        viewModel.isAddMenuVisible.observe(viewLifecycleOwner) {
            addMenu?.isVisible = it
        }

        viewModel.isCartMenuVisible.observe(viewLifecycleOwner) {
            cartMenu?.isVisible = it
        }

        viewModel.isSortMenuVisible.observe(viewLifecycleOwner) {
            sortMenu?.isVisible = it
        }

        viewModel.userWithWishList.observe(viewLifecycleOwner) { userWithWishList ->
            // Check if the user is customer then hide admin functions when true

            if (userWithWishList?.user != null) {
                when (userWithWishList.user.userType) {
                    UserType.CUSTOMER.name -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.updateAddMenu(false)
                            viewModel.updateCartMenu(true)
                            viewModel.updateSortMenu(true)
                        }

                        // Product Adapter For Customer
                        instantiateProductAdapterForCustomer()
                    }
                    UserType.ADMIN.name -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.updateAddMenu(true)
                            viewModel.updateCartMenu(false)
                            viewModel.updateSortMenu(false)
                        }

                        // Product Adapter For Admin
                        instantiateProductAdapterForAdmin()
                    }
                    else -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.updateAddMenu(false)
                            viewModel.updateCartMenu(false)
                            viewModel.updateSortMenu(true)
                        }
                    }
                }
                userAndWishList = userWithWishList
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.updateAddMenu(false)
                    viewModel.updateCartMenu(false)
                    viewModel.updateSortMenu(true)
                }

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            // Check if the user is admin then refresh the paging adapter every 10 seconds.
            while (true) {
                if (userAndWishList?.user != null && userAndWishList?.user!!.userType == UserType.ADMIN.name) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Refreshing Dataset Every 10 Seconds...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    delay(10_000)
                    adminAdapter.refresh()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.productsFlow.collectLatest {
                // For Admin. I can just pass 'it' No need for mapping wish lists.
                if (userAndWishList?.user != null) {
                    if (userAndWishList?.user!!.userType == UserType.ADMIN.name) {
                        adminAdapter.submitData(it)
                    } else {
                        showAdapterForCustomer(it)
                    }
                } else {
                    showAdapterForCustomer(it)
                }
            }

            viewModel.productEvent.collectLatest { event ->
                when (event) {
                    is ProductViewModel.ProductEvent.ShowMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adapter.refresh()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private suspend fun showAdapterForCustomer(it: PagingData<Product>) {
        val paging = it.map { p ->
            if (userAndWishList != null) {
                userAndWishList!!.wishList.forEach { w ->
                    p.isWishListedByUser = p.productId == w.productId
                }
            }
            p
        }
        adapter.submitData(paging)
    }

    override fun onResume() {
        requireActivity().invalidateOptionsMenu()
        Log.d("ProductFragment", "onResume: I'm called!!!")

        super.onResume()
    }

    private fun instantiateProductAdapterForCustomer() {
        binding.apply {
            recyclerProducts.setHasFixedSize(true)
            recyclerProducts.layoutManager = GridLayoutManager(requireContext(), 2)
            recyclerProducts.adapter = adapter
        }
    }

    private fun instantiateProductAdapterForAdmin() {
        binding.apply {
            recyclerProducts.setHasFixedSize(true)
            recyclerProducts.layoutManager = LinearLayoutManager(requireContext())
            recyclerProducts.adapter = adminAdapter
        }
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
        if (userAndWishList?.user != null) {
            viewModel.addToWishList(userAndWishList?.user!!.userId, product)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_action_menu, menu)

        // Set action add and cart to hide it whenever needed
        addMenu = menu.findItem(R.id.action_add)
        addMenu?.isVisible = viewModel.isAddMenuVisible.value!!

        cartMenu = menu.findItem(R.id.action_cart)
        cartMenu?.isVisible = viewModel.isCartMenuVisible.value!!

        sortMenu = menu.findItem(R.id.action_sort)
        sortMenu?.isVisible = viewModel.isSortMenuVisible.value!!

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
