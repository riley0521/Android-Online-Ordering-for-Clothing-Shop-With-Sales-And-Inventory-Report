package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.room.SessionPreferences
import com.teampym.onlineclothingshopapplication.data.room.SortOrder
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

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

    private var myMenu: Menu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adminAdapter = ProductAdminAdapter(this)

        lifecycleScope.launchWhenStarted {
            collectUserSession()

            viewModel.productEvent.collectLatest { event ->
                when (event) {
                    is ProductViewModel.ProductEvent.ShowSuccessMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adminAdapter.refresh()
                    }
                    is ProductViewModel.ProductEvent.ShowErrorMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            collectUserSession()
        }

        setHasOptionsMenu(true)
    }

    private suspend fun collectUserSession() {
        viewModel.userSession.collectLatest { userSession ->
            withContext(Dispatchers.Main) {
                Log.d(TAG, "onViewCreated: $userSession")
            }

            revalidateUser(userSession)

            viewModel.productsFlow.collectLatest {
                when (userSession.userType) {
                    UserType.ADMIN.name -> {
                        Log.d(TAG, "productsFlow: here")

                        adminAdapter.submitData(it)
                    }
                    UserType.CUSTOMER.name -> {
                        showAdapterForCustomer(it)
                    }
                    else -> {
                        showAdapterForCustomer(it)
                    }
                }
            }
        }
    }

    private fun revalidateUser(userSession: SessionPreferences) {
        // Check if the user is customer then hide admin functions when true

        Log.d(TAG, "onViewCreated: ${userSession.userType}")

        when (userSession.userType) {
            UserType.CUSTOMER.name -> {
                showAvailableMenus(userSession.userType)

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer(userSession.userId)
            }
            UserType.ADMIN.name -> {
                showAvailableMenus(userSession.userType)

                // Product Adapter For Admin
                instantiateProductAdapterForAdmin()
            }
            else -> {
                showAvailableMenus(userSession.userType)

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer(userSession.userId)
            }
        }
        viewModel.fetchWishList(userSession.userId)
    }

    private fun showAvailableMenus(userType: String) {
        Log.d(TAG, "showAvailableMenus: $userType")

        when (userType) {
            UserType.CUSTOMER.name -> {
                myMenu?.let {
                    it.findItem(R.id.action_add).isVisible = false
                    it.findItem(R.id.action_cart).isVisible = true
                    it.findItem(R.id.action_sort).isVisible = true
                }
            }
            UserType.ADMIN.name -> {
                myMenu?.let {
                    it.findItem(R.id.action_add).isVisible = true
                    it.findItem(R.id.action_cart).isVisible = false
                    it.findItem(R.id.action_sort).isVisible = false
                }
            }
            else -> {
                myMenu?.let {
                    it.findItem(R.id.action_add).isVisible = false
                    it.findItem(R.id.action_cart).isVisible = false
                    it.findItem(R.id.action_sort).isVisible = true
                }
            }
        }
    }

    private suspend fun showAdapterForCustomer(pagingData: PagingData<Product>) {
        Log.d(TAG, "showAdapterForCustomer: here")

        adapter.submitData(pagingData)
    }

    private fun instantiateProductAdapterForCustomer(userId: String?) {
        Log.d(TAG, "instantiateProductAdapterForCustomer: here")

        adapter = ProductAdapter(userId, this, requireContext())

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
            null
        )
        findNavController().navigate(action)
    }

    override fun onAddToWishListClicked(product: Product, isWishListed: Boolean) {
        if (viewModel.userWishList.value?.user != null) {
//            viewModel.addOrRemoveToWishList(
//                userAndWishList?.user!!.userId,
//                product,
//                currentPagingData!!,
//                ProductUpdateRemove.Edit(product),
//                isWishListed
//            )
        }
    }

    override fun onEditClicked(product: Product) {
        val action = ProductFragmentDirections.actionProductFragmentToAddEditProductFragment(
            "Edit Product (${product.productId})",
            product,
            true,
            product.categoryId
        )
        findNavController().navigate(action)
    }

    override fun onDeleteClicked(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("DELETE PRODUCT")
            .setMessage(
                "Are you sure you want to delete this product?\n" +
                    "All corresponding inventory, additional images, and reviews will be deleted as well."
            )
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.onDeleteProductClicked(
                    product
                )
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_action_menu, menu)

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
                // Navigate to add/edit product layout when admin
                val action = ProductFragmentDirections
                    .actionProductFragmentToAddEditProductFragment(categoryId = args.categoryId)
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}
