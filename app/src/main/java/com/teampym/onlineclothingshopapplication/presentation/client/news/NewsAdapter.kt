package com.teampym.onlineclothingshopapplication.presentation.client.news

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.REMOVE_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.NewsItemBinding
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

class NewsAdapter constructor(
    private val listener: NewsListener,
    private val context: Context,
    private val viewModel: NewsViewModel
) : PagingDataAdapter<Post, NewsAdapter.NewsViewHolder>(POST_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = NewsItemBinding.bind(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.news_item, parent, false)
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    companion object {
        val POST_COMPARATOR = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Post, newItem: Post) =
                oldItem == newItem
        }
    }

    inner class NewsViewHolder(private val binding: NewsItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnLike.setOnClickListener {
                likeClick()
            }

            binding.btnComment.setOnClickListener {
                commentClick()
            }

            binding.imgMenu.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position)
                    if (post != null) {
                        val showPopUpMenu = PopupMenu(
                            context,
                            binding.imgMenu
                        )

                        showPopUpMenu.menu.add(Menu.NONE, 1, 1, REMOVE_BUTTON)

                        showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                1 -> {
                                    AlertDialog.Builder(context)
                                        .setTitle("DELETE POST")
                                        .setMessage("Are you sure you want to delete this post?")
                                        .setPositiveButton("Yes") { _, _ ->
                                            viewModel.onViewEvent(
                                                NewsFragment.NewsPagerEvent.Remove(
                                                    post
                                                )
                                            )
                                            viewModel.onDeletePostClicked(post)
                                        }.setNegativeButton("No") { dialog, _ ->
                                            dialog.dismiss()
                                        }.show()
                                    true
                                }
                                else -> false
                            }
                        }

                        showPopUpMenu.show()
                    }
                }
            }
        }

        private fun commentClick() {
            val position = absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = getItem(position)
                if (item != null) {
                    listener.onCommentClicked(item)
                }
            }
        }

        private fun likeClick() {
            val position = absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = getItem(position)
                if (item != null) {
                    val isLiked = !item.isLikedByCurrentUser

                    viewModel.onViewEvent(
                        NewsFragment.NewsPagerEvent.Update(
                            item,
                            isLiked
                        )
                    )
                    viewModel.onLikePostClicked(item, isLiked)
                }
            }
        }

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        fun bind(item: Post) {
            binding.apply {
                Glide.with(itemView)
                    .load(item.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgAddedImage)

                Glide.with(itemView)
                    .load(item.avatarUrl)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_user)
                    .into(imgAvatar)

                txtCreatedBy.text = item.createdBy

                val calendarDate = Calendar.getInstance()
                calendarDate.timeInMillis = item.dateCreated
                calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
                val formattedDate = SimpleDateFormat("MM/dd/yyyy hh:mm:ss a")
                    .format(calendarDate.time)
                txtDateCreated.text = formattedDate

                txtTitle.text = item.title
                txtDescription.text = item.description

                if (viewModel.userType == UserType.ADMIN.name) {
                    imgMenu.isVisible = item.userId == viewModel.userId

                    imgLike.isVisible = false
                    tvLike.isVisible = false

                    imgComment.isVisible = false
                    tvComment.isVisible = false
                }

                if (item.haveUserId) {
                    if (item.isLikedByCurrentUser) {
                        imgLike.setColorFilter(ContextCompat.getColor(context, R.color.purple_500))
                        tvLike.setTextColor(ContextCompat.getColor(context, R.color.purple_500))

                        tvLike.text = context.getString(R.string.btn_liked)
                    } else {
                        imgLike.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                android.R.color.black
                            )
                        )
                        tvLike.setTextColor(ContextCompat.getColor(context, android.R.color.black))

                        tvLike.text = context.getString(R.string.btn_like)
                    }
                } else {
                    imgLike.isVisible = false
                    tvLike.isVisible = false
                }

                if (item.numberOfLikes == 0L) {
                    tvLikeDescription.visibility = View.INVISIBLE
                } else if (item.numberOfLikes == 1L) {
                    if (item.isLikedByCurrentUser) {
                        tvLikeDescription.isVisible = true
                        tvLikeDescription.text = "You liked this post."
                    } else {
                        tvLikeDescription.isVisible = true
                        tvLikeDescription.text = "1 person liked this post."
                    }
                } else if (item.numberOfLikes > 1L) {
                    if (item.isLikedByCurrentUser) {
                        tvLikeDescription.isVisible = true
                        tvLikeDescription.text = context.getString(
                            R.string.placeholder_like_description,
                            "You, and",
                            item.numberOfLikes - 1
                        )
                    } else {
                        tvLikeDescription.isVisible = true
                        tvLikeDescription.text = "${item.numberOfLikes} people liked this post."
                    }
                }

                tvComment.text = context.getString(
                    R.string.placeholder_comment,
                    item.numberOfComments
                )
            }
        }
    }

    interface NewsListener {
        fun onCommentClicked(item: Post)
        fun onShareClicked(item: Post)
    }
}
