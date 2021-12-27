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
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

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

    @Inject
    lateinit var db: FirebaseFirestore

    private var userAndWishList: UserWithWishList? = null

    private lateinit var loadingDialog: LoadingDialog

    private var addMenu: MenuItem? = null
    private var cartMenu: MenuItem? = null
    private var sortMenu: MenuItem? = null

    private var currentPagingData: PagingData<Product>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adminAdapter = ProductAdminAdapter(this)

        viewModel.updateCategoryId(args.categoryId)

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
                        viewModel.updateAddMenu(false)
                        viewModel.updateCartMenu(true)
                        viewModel.updateSortMenu(true)

                        // Product Adapter For Customer
                        instantiateProductAdapterForCustomer(userWithWishList.user.userId)
                    }
                    UserType.ADMIN.name -> {
                        viewModel.updateAddMenu(true)
                        viewModel.updateCartMenu(false)
                        viewModel.updateSortMenu(false)

                        // Product Adapter For Admin
                        instantiateProductAdapterForAdmin()
                    }
                    else -> {
                        viewModel.updateAddMenu(false)
                        viewModel.updateCartMenu(false)
                        viewModel.updateSortMenu(true)

                        // Product Adapter For Customer
                        instantiateProductAdapterForCustomer(userWithWishList.user.userId)
                    }
                }
                userAndWishList = userWithWishList
            } else {
                viewModel.updateAddMenu(false)
                viewModel.updateCartMenu(false)
                viewModel.updateSortMenu(true)

                // Product Adapter For Customer
                instantiateProductAdapterForCustomer(null)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.productsFlow.collectLatest {
                if (userAndWishList?.user != null) {
                    if (userAndWishList?.user!!.userType == UserType.ADMIN.name) {
                        Log.d(TAG, "productsFlow: here")

                        currentPagingData = it
                        adminAdapter.submitData(currentPagingData!!)
                    } else {
                        showAdapterForCustomer(it)
                    }
                } else {
                    showAdapterForCustomer(it)
                }
            }

            viewModel.productEvent.collectLatest { event ->
                when (event) {
                    is ProductViewModel.ProductEvent.ShowSuccessMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adminAdapter.submitData(event.currentPagingData)
                    }
                    is ProductViewModel.ProductEvent.ShowErrorMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    is ProductViewModel.ProductEvent.ShowAddedToWishListSuccessMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                        adapter.submitData(event.currentPagingData)
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private suspend fun showAdapterForCustomer(pagingData: PagingData<Product>) {
        Log.d(TAG, "showAdapterForCustomer: here")

        currentPagingData = pagingData
        adapter.submitData(currentPagingData!!)
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

    override fun onResume() {
        requireActivity().invalidateOptionsMenu()
        Log.d(TAG, "onResume: I'm called!!!")

        super.onResume()
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
        if (userAndWishList?.user != null && currentPagingData != null) {
            viewModel.addOrRemoveToWishList(
                userAndWishList?.user!!.userId,
                product,
                currentPagingData!!,
                ProductUpdateRemove.Edit(product),
                isWishListed
            )
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
                if (currentPagingData != null) {
                    loadingDialog.show()
                    viewModel.onDeleteProductClicked(
                        product,
                        currentPagingData!!,
                        ProductUpdateRemove.Remove(product)
                    )
                }
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
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
                // Navigate to add/edit product layout when admin
                val action = ProductFragmentDirections
                    .actionProductFragmentToAddEditProductFragment(categoryId = args.categoryId)
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }

    sealed class ProductUpdateRemove {
        data class Edit(val product: Product) : ProductUpdateRemove()
        data class Remove(val product: Product) : ProductUpdateRemove()
    }
}
