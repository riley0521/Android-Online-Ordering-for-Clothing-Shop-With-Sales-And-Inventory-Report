package com.teampym.onlineclothingshopapplication.presentation.client.categories

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.databinding.FragmentCategoryBinding
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "CategoryFragment"

@AndroidEntryPoint
class CategoryFragment : Fragment(R.layout.fragment_category), CategoryAdapter.OnCategoryListener {

    private lateinit var binding: FragmentCategoryBinding

    private lateinit var adapter: CategoryAdapter

    private val categoryViewModel by viewModels<CategoryViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCategoryBinding.bind(view)

        FirebaseAuth.getInstance().currentUser?.let {
            categoryViewModel.updateUserId(it.uid)
        }

        adapter = CategoryAdapter(this)

        binding.apply {
            recyclerCategories.setHasFixedSize(true)
            recyclerCategories.adapter = adapter
        }

        categoryViewModel.loadCategories().observe(viewLifecycleOwner) {
            adapter.submitList(it)
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
}
