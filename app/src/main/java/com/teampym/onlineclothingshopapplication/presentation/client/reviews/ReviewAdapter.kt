package com.teampym.onlineclothingshopapplication.presentation.client.reviews

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.databinding.ReviewItemBinding
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class ReviewAdapter(
    private val context: Context
) : PagingDataAdapter<Review, ReviewAdapter.ReviewViewHolder>(REVIEW_COMPARATOR) {

    companion object {
        val REVIEW_COMPARATOR = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Review, newItem: Review) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ReviewItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class ReviewViewHolder(private val binding: ReviewItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {

        @SuppressLint("SimpleDateFormat")
        fun bind(review: Review) {
            binding.apply {
                Glide.with(itemView)
                    .load(review.userAvatar)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_user)
                    .into(imgAvatar)

                tvUsername.text = review.username

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = review.dateReview
                calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val formattedDate = SimpleDateFormat("MM/dd/yyyy")
                    .format(calendarDate.time)
                tvDate.text = formattedDate

                tvVariant.text = context.getString(R.string.placeholder_variant_size, review.productSize)

                productRating.rating = review.rate.toFloat()

                tvReview.text = review.description
            }
        }
    }
}
