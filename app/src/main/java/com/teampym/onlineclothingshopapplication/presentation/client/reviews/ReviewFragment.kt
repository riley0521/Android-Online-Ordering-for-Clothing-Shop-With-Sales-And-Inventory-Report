package com.teampym.onlineclothingshopapplication.presentation.client.reviews

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentReviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReviewsFragment : Fragment(R.layout.fragment_review) {

    private lateinit var binding: FragmentReviewBinding

    private val args by navArgs<ReviewsFragmentArgs>()

    private val viewModel by viewModels<ReviewViewModel>()

    private lateinit var adapter: ReviewAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentReviewBinding.bind(view)

        adapter = ReviewAdapter(requireContext())

        lifecycleScope.launchWhenStarted {
            viewModel.fetchUserSessionAndReviews(args.productId)

            setupViews()
        }
    }

    private fun setupViews() = CoroutineScope(Dispatchers.Main).launch {
        viewModel.reviews?.observe(viewLifecycleOwner) {
            binding.refreshLayout.isRefreshing = false

            it?.let { pagingData ->
                adapter.submitData(viewLifecycleOwner.lifecycle, pagingData)
            }
        }

        binding.apply {
            rvReviews.setHasFixedSize(true)
            rvReviews.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            rvReviews.adapter = adapter

            refreshLayout.setOnRefreshListener {
                adapter.refresh()
            }
        }
    }
}
