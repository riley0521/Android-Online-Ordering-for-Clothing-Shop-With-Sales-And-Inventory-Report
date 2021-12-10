package com.teampym.onlineclothingshopapplication.presentation.client.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.Menu
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.CANCEL_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.EDIT_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.REMOVE_BUTTON
import com.teampym.onlineclothingshopapplication.data.util.SUGGEST_BUTTON
import com.teampym.onlineclothingshopapplication.databinding.CategoryItemBinding

class CategoryAdapter(
    private val listener: OnCategoryListener,
    private val context: Context
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(
    CATEGORY_COMPARATOR
) {

    companion object {
        private val CATEGORY_COMPARATOR = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding =
            CategoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)

        if (category != null)
            holder.bind(category)
    }

    inner class CategoryViewHolder(private val binding: CategoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null)
                        listener.onItemClick(item)
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        val showPopUpMenu = PopupMenu(
                            context,
                            binding.root
                        )

                        showPopUpMenu.menu.add(Menu.NONE, 0, 0, EDIT_BUTTON)
                        showPopUpMenu.menu.add(Menu.NONE, 1, 1, REMOVE_BUTTON)

                        showPopUpMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                0 -> {
                                    listener.onEditClicked(item)
                                    true
                                }
                                1 -> {
                                    listener.onDeleteClicked(item)
                                    true
                                }
                                else -> false
                            }
                        }
                    }
                }
                true
            }
        }

        fun bind(category: Category) {
            binding.apply {
                Glide.with(imageCategory)
                    .load(category.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imageCategory)

                tvCategoryName.text = category.name
            }
        }
    }

    interface OnCategoryListener {
        fun onItemClick(category: Category)
        fun onEditClicked(category: Category)
        fun onDeleteClicked(category: Category)
    }
}
