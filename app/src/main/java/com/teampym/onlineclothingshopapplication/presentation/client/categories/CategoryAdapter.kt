package com.teampym.onlineclothingshopapplication.presentation.client.categories

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Category
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.CategoryItemBinding

class CategoryAdapter(
    private val listener: OnCategoryListener,
    private val userType: String
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
        RecyclerView.ViewHolder(binding.root),
        View.OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null)
                        listener.onItemClick(item)
                }
            }

            if (userType == UserType.ADMIN.name) {
                binding.root.setOnCreateContextMenuListener(this)
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

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.setHeaderTitle("Select Action")
            val edit = menu?.add(Menu.NONE, 1, 1, "Edit")
            val delete = menu?.add(Menu.NONE, 2, 2, "Delete")

            edit?.setOnMenuItemClickListener(this)
            delete?.setOnMenuItemClickListener(this)
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            val position = absoluteAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val category = getItem(position)

                if (category != null) {
                    when (item?.itemId) {
                        1 -> {
                            listener.onEditClicked(category)
                            return true
                        }
                        2 -> {
                            listener.onDeleteClicked(category, position)
                            return true
                        }
                    }
                }
            }
            return false
        }
    }

    interface OnCategoryListener {
        fun onItemClick(category: Category)
        fun onEditClicked(category: Category)
        fun onDeleteClicked(category: Category, position: Int)
    }
}
