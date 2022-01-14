package com.teampym.onlineclothingshopapplication.presentation.client.request_return_item

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UploadedImage
import com.teampym.onlineclothingshopapplication.databinding.ProductImageItemBinding

class RequestReturnItemAdapter(
    private val listener: RequestReturnListener,
    private val list: List<Any>
) : RecyclerView.Adapter<RequestReturnItemAdapter.RequestReturnImageViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RequestReturnImageViewHolder {
        val binding = ProductImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RequestReturnImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestReturnImageViewHolder, position: Int) {
        val item = list[position]

        holder.bind(item)
    }

    override fun getItemCount(): Int = list.size

    inner class RequestReturnImageViewHolder(private val binding: ProductImageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.imgRemove.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onRemoveClicked(position)
                }
            }
        }

        fun bind(image: Any) {
            when (image) {
                is Uri -> {
                    Glide.with(itemView)
                        .load(image)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_food)
                        .into(binding.imgProductShots)
                }
                is UploadedImage -> {
                    Glide.with(itemView)
                        .load(image.url)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_food)
                        .into(binding.imgProductShots)

                    binding.imgRemove.isVisible = false
                }
            }
        }
    }

    interface RequestReturnListener {
        fun onRemoveClicked(position: Int)
    }
}
