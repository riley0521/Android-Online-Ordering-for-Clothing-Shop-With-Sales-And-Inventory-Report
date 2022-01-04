package com.teampym.onlineclothingshopapplication.presentation.client.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.databinding.PagingLoadStateBinding

class ProductLoadStateAdapter(
    private val adapter: Any
) : LoadStateAdapter<ProductLoadStateAdapter.ProductLoadStateViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): ProductLoadStateViewHolder {
        val binding = PagingLoadStateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductLoadStateViewHolder(binding) {
            when (adapter) {
                is ProductAdapter -> {
                    adapter.retry()
                }
                is ProductAdminAdapter -> {
                    adapter.retry()
                }
                else -> Unit
            }
        }
    }

    override fun onBindViewHolder(holder: ProductLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    inner class ProductLoadStateViewHolder(
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
