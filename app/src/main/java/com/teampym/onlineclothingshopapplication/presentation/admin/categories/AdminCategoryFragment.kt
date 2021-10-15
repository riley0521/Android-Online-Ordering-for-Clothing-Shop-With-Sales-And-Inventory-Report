package com.teampym.onlineclothingshopapplication.presentation.admin.categories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.teampym.onlineclothingshopapplication.R

class AdminCategoryFragment : Fragment() {
    private val viewModel: AdminCategoryViewModel by viewModels()

    //private lateinit var binding: FragmentAdminCategoryBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding = FragmentAdminCategoryBinding.bind(view)
    }
}