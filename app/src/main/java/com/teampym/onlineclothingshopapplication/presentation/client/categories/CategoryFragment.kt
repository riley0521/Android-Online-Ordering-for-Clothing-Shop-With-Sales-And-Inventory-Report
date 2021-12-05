package com.teampym.onlineclothingshopapplication.presentation.client.categories

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentCategoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class CategoryFragment : Fragment(R.layout.fragment_category), CategoryAdapter.OnCategoryListener {

    private lateinit var binding: FragmentCategoryBinding

    private lateinit var adapter: CategoryAdapter

    private val viewModel by viewModels<CategoryViewModel>()

    private lateinit var loadingDialog: LoadingDialog

    private var myMenu: Menu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCategoryBinding.bind(view)

        loadingDialog = LoadingDialog(requireActivity())
        loadingDialog.show()

        viewModel.loadCategories()
        FirebaseAuth.getInstance().currentUser?.let {
            viewModel.updateUserId(it.uid)
        }

        adapter = CategoryAdapter(this)

        binding.apply {
            recyclerCategories.setHasFixedSize(true)
            recyclerCategories.adapter = adapter
        }

        viewModel.categories.observe(viewLifecycleOwner) {
            if (loadingDialog.isActive()) {
                loadingDialog.dismiss()
            }
            adapter.submitList(it)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.userFlow.collectLatest { user ->
                when (user.userType) {
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
                        }
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onItemClick(category: Category) {
        val action =
            CategoryFragmentDirections.actionCategoryFragmentToProductFragment(
                category.name,
                category.id
            )
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.category_action_menu, menu)

        myMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_category -> {
                // TODO("Navigate to add category layout when admin")
                true
            }
            else -> false
        }
    }
}
