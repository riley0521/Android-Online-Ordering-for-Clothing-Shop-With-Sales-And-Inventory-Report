package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.databinding.ImageItemBinding

class ImagePagerAdapter(private val context: Context) :
    ListAdapter<ProductImage, ImagePagerAdapter.ImageViewHolder>(IMAGE_COMPARATOR) {

    companion object {
        private val IMAGE_COMPARATOR = object : DiffUtil.ItemCallback<ProductImage>() {
            override fun areItemsTheSame(oldItem: ProductImage, newItem: ProductImage) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ProductImage, newItem: ProductImage) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ImageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(
            getItem(position).imageUrl,
            context.getString(R.string.count_indicator_text, position + 1, currentList.size)
        )
    }

    override fun onViewAttachedToWindow(holder: ImageViewHolder) {
        holder.onViewAppear()

        super.onViewAttachedToWindow(holder)
    }

    inner class ImageViewHolder(private val binding: ImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String, countIndicatorText: String) {
            binding.apply {
                Glide.with(itemView)
                    .load(imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(carouselImageView)

                imagePositionIndicatorTextView.text = countIndicatorText
            }
        }

        fun onViewAppear() {
            binding.imagePositionIndicatorTextView.alpha = 1.0f
            fadeAwayIndicatorTextViewWithDelay(2000, 2000)
        }

        private fun fadeAwayIndicatorTextViewWithDelay(
            fadeDurationInMillis: Long,
            delayInMillis: Long
        ) {
            ObjectAnimator.ofFloat(binding.imagePositionIndicatorTextView, "alpha", 1f, 0f).apply {
                duration = fadeDurationInMillis
                startDelay = delayInMillis
            }.start()
        }
    }
}
