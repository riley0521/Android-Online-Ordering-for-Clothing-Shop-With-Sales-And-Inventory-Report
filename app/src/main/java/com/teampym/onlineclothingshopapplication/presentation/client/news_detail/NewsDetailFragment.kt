package com.teampym.onlineclothingshopapplication.presentation.client.news_detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentNewsDetailBinding

class NewsDetailFragment : Fragment(R.layout.fragment_news_detail) {

    private lateinit var binding: FragmentNewsDetailBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentNewsDetailBinding.bind(view)
    }
}
