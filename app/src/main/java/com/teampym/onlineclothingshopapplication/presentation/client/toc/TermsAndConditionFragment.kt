package com.teampym.onlineclothingshopapplication.presentation.client.toc

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentTermsAndConditionBinding

class TermsAndConditionFragment : Fragment(R.layout.fragment_terms_and_condition) {

    private lateinit var binding: FragmentTermsAndConditionBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTermsAndConditionBinding.bind(view)
    }
}
