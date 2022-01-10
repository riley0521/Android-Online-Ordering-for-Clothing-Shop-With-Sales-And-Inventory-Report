package com.teampym.onlineclothingshopapplication.presentation.client.size

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.SizeChart
import com.teampym.onlineclothingshopapplication.databinding.SizeChartImageItemBinding

class SizeChartImageAdapter(
    private val listener: SizeChartImageListener,
    private val showRemoveButton: Boolean = false
) : ListAdapter<SizeChart, SizeChartImageAdapter.SizeChartImageViewHolder>(SIZE_CHART_COMPARATOR) {

    companion object {
        val SIZE_CHART_COMPARATOR = object : DiffUtil.ItemCallback<SizeChart>() {
            override fun areItemsTheSame(oldItem: SizeChart, newItem: SizeChart) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SizeChart, newItem: SizeChart) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SizeChartImageViewHolder {
        val binding = SizeChartImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SizeChartImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SizeChartImageViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    inner class SizeChartImageViewHolder(private val binding: SizeChartImageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.imgRemove.setOnClickListener {
                val position = absoluteAdapterPosition
                if(position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if(item != null) {
                        listener.onRemoveClicked(item, position)
                    }
                }
            }
        }

        fun bind(sizeChart: SizeChart) {
            Glide.with(itemView)
                .load(sizeChart.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_user)
                .into(binding.imgSizeChart)

            binding.imgRemove.isVisible = showRemoveButton
        }
    }

    interface SizeChartImageListener {
        fun onRemoveClicked(sizeChart: SizeChart, position: Int)
    }
}
