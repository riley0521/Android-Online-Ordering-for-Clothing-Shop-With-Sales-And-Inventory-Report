package com.teampym.onlineclothingshopapplication.presentation.faqs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.data.models.FAQModel
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FaqItemBinding

class FrequentlyAskQuestionsAdapter(
    private val listener: FrequentlyAskQuestionsListener,
    private val userType: String
) : ListAdapter<FAQModel, FrequentlyAskQuestionsAdapter.FrequentlyAskQuestionsViewHolder>(
    FAQ_COMPARATOR
) {

    companion object {
        val FAQ_COMPARATOR = object : DiffUtil.ItemCallback<FAQModel>() {
            override fun areItemsTheSame(oldItem: FAQModel, newItem: FAQModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FAQModel, newItem: FAQModel) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FrequentlyAskQuestionsViewHolder {
        val binding = FaqItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FrequentlyAskQuestionsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FrequentlyAskQuestionsViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            holder.bind(item)
        }
    }

    inner class FrequentlyAskQuestionsViewHolder(private val binding: FaqItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        listener.onItemClicked(item)
                    }
                }
            }

            binding.imgDelete.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        listener.onDeleteClicked(item, position)
                    }
                }
            }
        }

        fun bind(faq: FAQModel) {
            binding.apply {
                tvQuestion.text = faq.question
                tvAnswer.text = faq.answer

                imgDelete.isVisible = userType == UserType.ADMIN.name
            }
        }
    }

    interface FrequentlyAskQuestionsListener {
        fun onItemClicked(faq: FAQModel)
        fun onDeleteClicked(faq: FAQModel, position: Int)
    }
}
