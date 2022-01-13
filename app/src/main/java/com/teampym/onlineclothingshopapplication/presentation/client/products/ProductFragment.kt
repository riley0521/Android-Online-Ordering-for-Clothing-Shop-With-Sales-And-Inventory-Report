package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Inventory
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SessionPreferences
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_news.*
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "ProductFragment"

@AndroidEntryPoint
class ProductFragment :
    Fragment(R.layout.fragment_product),
    ProductAdapter.OnProductListener,
    ProductAdminAdapter.OnProductAdminListener {

    private lateinit var binding: FragmentProductBinding

    private lateinit var adapter: ProductAdapter
    private lateinit var adminAdapter: ProductAdminAdapter

    private lateinit var searchView: SearchView

    private val viewModel: ProductViewModel by viewModels()

    private val args by navArgs<ProductFragmentArgs>()

    private lateinit var loadingDialog: LoadingDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adminAdapter = ProductAdminAdapter(requireContext(), this)
        adminAdapter.withLoadStateHeaderAndFooter(
            header = ProductLoadStateAdapter(adminAdapter),
            footer = ProductLoadStateAdapter(adminAdapter)
        )
        adminAdapter.addLoadStateListener {
            if (it.source.refresh is LoadState.NotLoading && it.append.endOfPaginationReached) {
                if (adminAdapter.itemCount < 1) {
                    binding.recyclerProducts.visibility = View.INVISIBLE
                    binding.tvNoProducts.visibility = View.VISIBLE
                    binding.tvNoProducts.text =
                        getString(R.string.label_no_products_yet_for_this_category_come_back_later)
                } else {
                    binding.recyclerProducts.visibility = View.VISIBLE
                    binding.tvNoProducts.isVisible = false
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.productEvent.collectLatest { event ->
                when (event) {
                    is ProductViewModel.ProductEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adminAdapter.refresh()
                    }
                    is ProductViewModel.ProductEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().invalidateOptionsMenu()
    }

    private fun showAdapterForCustomer(pagingData: PagingData<Product>) {
        Log.d(TAG, "showAdapterForCustomer: here")

        adapter.submitData(viewLifecycleOwner.lifecycle, pagingData)
    }

    private fun instantiateProductAdapterForCustomer() {
        Log.d(TAG, "instantiateProductAdapterForCustomer: here")

        adapter = ProductAdapter(this, requireContext())
        adapter.withLoadStateHeaderAndFooter(
            header = ProductLoadStateAdapter(adapter),
            footer = ProductLoadStateAdapter(adapter)
        )
        adapter.addLoadStateListener {
            if (it.source.refresh is LoadState.NotLoading && it.append.endOfPaginationReached) {
                if (adapter.itemCount < 1) {

                    if (!searchView.isIconified) {
                        binding.recyclerProducts.visibility = View.INVISIBLE
                        binding.tvNoProducts.visibility = View.VISIBLE
                        binding.tvNoProducts.text =
                            "We could not find any products for ${viewModel.searchQuery.value}"
                    } else {
                        binding.recyclerProducts.visibility = View.INVISIBLE
                        binding.tvNoProducts.visibility = View.VISIBLE
                        binding.tvNoProducts.text =
                            getString(R.string.label_no_products_yet_for_this_category_come_back_later)
                    }
                } else {
                    binding.recyclerProducts.visibility = View.VISIBLE
                    binding.tvNoProducts.isVisible = false
                }
            }
        }

        binding.apply {
            recyclerProducts.setHasFixedSize(true)
            recyclerProducts.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            recyclerProducts.adapter = adapter
        }
    }

    private fun instantiateProductAdapterForAdmin() {
        Log.d(TAG, "instantiateProductAdapterForAdmin: here")

        binding.apply {
            recyclerProducts.setHasFixedSize(true)
            recyclerProducts.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            recyclerProducts.adapter = adminAdapter
        }
    }

    override fun onItemClicked(product: Product) {
        val action = ProductFragmentDirections.actionProductFragmentToProductDetailFragment(
            product,
            product.name,
            product.productId
        )
        findNavController().navigate(action)
    }

    override fun onEditClicked(product: Product) {
        val action = ProductFragmentDirections.actionProductFragmentToAddEditProductFragment(
            "Edit Product (${product.name})",
            product,
            true,
            product.categoryId,
            args.title
        )
        findNavController().navigate(action)
    }

    override fun onDeleteProductClicked(product: Product) {
        loadingDialog.show()
        viewModel.onDeleteProductClicked(
            product
        )
    }

    override fun onDeleteSizeClicked(inventory: Inventory, productName: String) {
        loadingDialog.dismiss()
        viewModel.deleteSize(inventory, productName)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_action_menu, menu)

        viewModel.getUserSession().observe(viewLifecycleOwner) {
            collectUserSession(it, menu)
        }

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

        searchView.setOnCloseListener {
            viewModel.searchQuery.value = ""
            true
        }
    }

    private fun collectUserSession(userSession: SessionPreferences, menu: Menu) {
        // Check if the user is customer then hide admin functions when true

        when (userSession.userType) {
            UserType.CUSTOMER.name -> {
                showAvailableMenus(userSession.userType, menu)

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer()
            }
            UserType.ADMIN.name -> {
                showAvailableMenus(userSession.userType, menu)

                // Product Adapter For Admin
                instantiateProductAdapterForAdmin()
            }
            else -> {
                showAvailableMenus(userSession.userType, menu)

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer()
            }
        }

        collectProductPagingData(userSession)
    }

    @SuppressLint("SetTextI18n")
    private fun collectProductPagingData(userSession: SessionPreferences) {
        viewModel.products.observe(viewLifecycleOwner) { pagingData ->
            refreshLayout.isRefreshing = false

            when (userSession.userType) {
                UserType.ADMIN.name -> {
                    Log.d(TAG, "productsFlow: here")

                    adminAdapter.submitData(viewLifecycleOwner.lifecycle, pagingData)
                }
                UserType.CUSTOMER.name -> {
                    showAdapterForCustomer(pagingData)
                }
                else -> {
                    showAdapterForCustomer(pagingData)
                }
            }
        }

        Log.d(TAG, "collectProductPagingData: calling binding object")

        binding.apply {
            refreshLayout.setOnRefreshListener {
                Log.d(TAG, "setOnRefreshListener: here")
                if (this@ProductFragment::adapter.isInitialized) {
                    adapter.refresh()
                }
                if (this@ProductFragment::adminAdapter.isInitialized) {
                    adminAdapter.refresh()
                }
            }
        }
    }

    private fun showAvailableMenus(userType: String, menu: Menu) {
        Log.d(TAG, "showAvailableMenus: $userType")

        when (userType) {
            UserType.CUSTOMER.name -> {
                menu.findItem(R.id.action_add).isVisible = false
                menu.findItem(R.id.action_cart).isVisible = true
                menu.findItem(R.id.action_sort).isVisible = true
            }
            UserType.ADMIN.name -> {
                menu.findItem(R.id.action_add).isVisible = true
                menu.findItem(R.id.action_cart).isVisible = false
                menu.findItem(R.id.action_sort).isVisible = false
            }
            else -> {
                menu.findItem(R.id.action_add).isVisible = false
                menu.findItem(R.id.action_cart).isVisible = false
                menu.findItem(R.id.action_sort).isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_no_sort -> {
                viewModel.updateSortOrder(SortOrder.BY_NAME)
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
                // Navigate to add/edit product layout when admin
                val action = ProductFragmentDirections
                    .actionProductFragmentToAddEditProductFragment(
                        categoryId = args.categoryId,
                        title = "Add New Product",
                        editMode = false,
                        categoryName = args.title,
                        product = null
                    )
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}
