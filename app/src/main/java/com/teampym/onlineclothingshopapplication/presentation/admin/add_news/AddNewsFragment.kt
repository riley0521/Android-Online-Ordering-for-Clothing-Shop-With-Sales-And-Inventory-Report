package com.teampym.onlineclothingshopapplication.presentation.admin.add_news

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddNewsBinding

class AddNewsFragment : Fragment(R.layout.fragment_add_news) {

    private lateinit var binding: FragmentAddNewsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddNewsBinding.bind(view)
    }
}
