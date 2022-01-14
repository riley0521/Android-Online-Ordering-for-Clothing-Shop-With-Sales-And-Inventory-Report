package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.models.getDate
import com.teampym.onlineclothingshopapplication.databinding.ReviewItemBinding

class ReviewAdapter(
    private val context: Context
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(REVIEW_COMPARATOR) {

    companion object {
        private val REVIEW_COMPARATOR = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Review, newItem: Review) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class ReviewViewHolder(private val binding: ReviewItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {

            binding.apply {
                Glide.with(itemView)
                    .load(review.userAvatar)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_user)
                    .into(imgAvatar)

                tvUsername.text = review.username
                tvReview.text = review.description
                tvDate.text = getDate(review.dateReview)
                tvVariant.text = context.getString(
                    R.string.placeholder_variant_size,
                    review.productSize
                )

                productRating.rating = review.rate.toFloat()
            }
        }
    }
}
