package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.PagingLoadStateBinding

class OrderListLoadStateAdapter(
    private val adapter: OrderListAdapter
) : LoadStateAdapter<OrderListLoadStateAdapter.OrderListLoadStateViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): OrderListLoadStateViewHolder {
        val binding = PagingLoadStateBinding.bind(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.paging_load_state, parent, false)
        )
        return OrderListLoadStateViewHolder(binding) {
            adapter.retry()
        }
    }

    override fun onBindViewHolder(holder: OrderListLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    inner class OrderListLoadStateViewHolder(
        private val binding: PagingLoadStateBinding,
        private val retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRetry.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) {
            with(binding) {
                progressBar.isVisible = loadState is LoadState.Loading
                btnRetry.isVisible = loadState is LoadState.Error
                tvError.isVisible = loadState is LoadState.Error
            }
        }
    }
}