package com.teampym.onlineclothingshopapplication.presentation.faqs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentFrequentlyAskQuestionsBinding

class FrequentlyAskQuestionsFragment : Fragment(R.layout.fragment_frequently_ask_questions) {

    private lateinit var binding: FragmentFrequentlyAskQuestionsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFrequentlyAskQuestionsBinding.bind(view)
    }
}
