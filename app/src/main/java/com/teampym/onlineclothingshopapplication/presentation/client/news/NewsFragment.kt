package com.teampym.onlineclothingshopapplication.presentation.client.news

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentNewsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "NewsFragment"

@AndroidEntryPoint
class NewsFragment : Fragment(R.layout.fragment_news), NewsAdapter.NewsListener {

    private lateinit var binding: FragmentNewsBinding

    private lateinit var adapter: NewsAdapter

    private val viewModel by viewModels<NewsViewModel>()

    private var currentPagingData: PagingData<Post>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentNewsBinding.bind(view)

        adapter = NewsAdapter(this, requireContext(), viewModel)
        adapter.withLoadStateHeaderAndFooter(
            header = NewsLoadStateAdapter(adapter),
            footer = NewsLoadStateAdapter(adapter)
        )

        CoroutineScope(Dispatchers.IO).launch {
            viewModel.fetchUserSession()
        }.invokeOnCompletion {
            setupViews()
        }
    }

    private fun setupViews() {
        viewModel.getPostsPagingData()

        binding.apply {
            refreshLayout.setOnRefreshListener {
                adapter.refresh()
            }

            recyclerNews.setHasFixedSize(true)
            recyclerNews.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            recyclerNews.adapter = adapter
        }

        viewModel.postPagingData.observe(viewLifecycleOwner) {
            binding.refreshLayout.isRefreshing = false

            currentPagingData = it
            adapter.submitData(viewLifecycleOwner.lifecycle, it)
        }

        viewModel.userSession.observe(viewLifecycleOwner) {
            if (it.userType == UserType.ADMIN.name) {
                binding.fabNewPost.isVisible = true
                binding.fabNewPost.setOnClickListener {
                    Log.d(TAG, "fab: here")
                }
            }
        }
    }

    override fun onCommentClicked(item: Post) {
        Toast.makeText(
            requireContext(),
            "Post: ${item.title} go to comment section",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onShareClicked(item: Post) {
        Toast.makeText(
            requireContext(),
            "Post: ${item.title} clicked",
            Toast.LENGTH_SHORT
        ).show()
    }

    sealed class NewsPagerEvent {
        data class Update(val post: Post, val isLikeByUser: Boolean) : NewsPagerEvent()
    }
}
