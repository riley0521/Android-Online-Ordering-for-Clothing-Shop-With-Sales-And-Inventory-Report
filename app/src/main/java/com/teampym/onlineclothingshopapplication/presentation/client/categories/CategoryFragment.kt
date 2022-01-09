package com.teampym.onlineclothingshopapplication.presentation.client.categories

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "CategoryFragment"

@AndroidEntryPoint
class CategoryFragment : Fragment(R.layout.fragment_category), CategoryAdapter.OnCategoryListener {

    private lateinit var binding: FragmentCategoryBinding

    private lateinit var adapter: CategoryAdapter

    private val viewModel by viewModels<CategoryViewModel>()

    private lateinit var loadingDialog: LoadingDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCategoryBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())
        binding.refreshLayout.isRefreshing = true

        lifecycleScope.launchWhenStarted {
            setupViews(viewModel.session.first().userType)

            viewModel.categoryEvent.collectLatest { event ->
                when (event) {
                    is CategoryViewModel.CategoryEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is CategoryViewModel.CategoryEvent.ShowSuccessMessage -> {
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

        setHasOptionsMenu(true)
    }

    private fun setupViews(userType: String) = CoroutineScope(Dispatchers.Main).launch {
        adapter = CategoryAdapter(this@CategoryFragment, userType)

        viewModel.onLoadCategories()

        binding.apply {
            refreshLayout.setOnRefreshListener {
                viewModel.onLoadCategories()
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) {
            binding.refreshLayout.isRefreshing = false

            adapter.submitList(it)

            binding.recyclerCategories.setHasFixedSize(true)
            binding.recyclerCategories.adapter = adapter
        }
    }

    override fun onResume() {
        super.onResume()

        requireActivity().invalidateOptionsMenu()
    }

    override fun onItemClick(category: Category) {

        // Update categoryId in preferences
        viewModel.updateCategoryId(category.id)

        val action = CategoryFragmentDirections
            .actionCategoryFragmentToProductFragment(
                category.name,
                category.id
            )
        findNavController().navigate(action)
    }

    override fun onEditClicked(category: Category) {
        val action = CategoryFragmentDirections.actionCategoryFragmentToAddEditCategoryFragment(
            category,
            true,
            "Edit Category (${category.name})"
        )
        findNavController().navigate(action)
    }

    override fun onDeleteClicked(category: Category, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("DELETE CATEGORY")
            .setMessage(
                "Are you sure you want to delete this category?\n" +
                    "All corresponding products will be deleted as well."
            )
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.onDeleteCategoryClicked(category, position)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.category_action_menu, menu)

        viewModel.getUserSession().observe(viewLifecycleOwner) { session ->

            Log.d(TAG, "showAvailableMenus: $session")

            when (session.userType) {
                UserType.CUSTOMER.name -> {
                    menu.findItem(R.id.action_add).isVisible = false
                }
                UserType.ADMIN.name -> {
                    menu.findItem(R.id.action_add).isVisible = true
                }
                else -> {
                    menu.findItem(R.id.action_add).isVisible = false
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_category -> {
                val action =
                    CategoryFragmentDirections.actionCategoryFragmentToAddEditCategoryFragment()
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}
