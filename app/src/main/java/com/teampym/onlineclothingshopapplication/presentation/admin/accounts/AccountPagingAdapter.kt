package com.teampym.onlineclothingshopapplication.presentation.admin.accounts

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.UserStatus
import com.teampym.onlineclothingshopapplication.databinding.AccountItemBinding

class AccountPagingAdapter(
    private val listener: AccountListener,
    private val context: Context
) : PagingDataAdapter<UserInformation, AccountPagingAdapter.AccountViewHolder>(USER_COMPARATOR) {

    companion object {
        val USER_COMPARATOR = object : DiffUtil.ItemCallback<UserInformation>() {
            override fun areItemsTheSame(
                oldItem: UserInformation,
                newItem: UserInformation
            ) = oldItem.userId == newItem.userId

            override fun areContentsTheSame(
                oldItem: UserInformation,
                newItem: UserInformation
            ) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = AccountItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null) {
            holder.bind(item)
        }
    }

    inner class AccountViewHolder(private val binding: AccountItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnBanUser.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    if (item != null) {
                        when (binding.btnBanUser.text) {
                            context.getString(R.string.btn_ban_user) -> {
                                listener.onBanClicked(item)
                            }
                            context.getString(R.string.btn_un_ban_user) -> {
                                listener.onUnBanClicked(item)
                            }
                        }
                    }
                }
            }
        }

        fun bind(user: UserInformation) {
            binding.apply {
                tvUserId.text = context.getString(
                    R.string.placeholder_user_id,
                    user.userId
                )
                tvUsername.text = context.getString(
                    R.string.placeholder_username,
                    "${user.firstName} ${user.lastName}"
                )
                tvUserStatus.text = context.getString(
                    R.string.placeholder_user_status,
                    user.userStatus
                )

                if (user.userStatus == UserStatus.ACTIVE.name) {
                    btnBanUser.text = context.getString(R.string.btn_ban_user)
                } else {
                    btnBanUser.text = context.getString(R.string.btn_un_ban_user)
                }
            }
        }
    }

    interface AccountListener {
        fun onBanClicked(user: UserInformation)
        fun onUnBanClicked(user: UserInformation)
    }
}
