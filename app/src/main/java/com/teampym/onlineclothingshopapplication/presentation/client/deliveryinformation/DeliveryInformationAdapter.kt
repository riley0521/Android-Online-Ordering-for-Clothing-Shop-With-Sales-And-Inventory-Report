package com.teampym.onlineclothingshopapplication.presentation.client.deliveryinformation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformation
import com.teampym.onlineclothingshopapplication.databinding.DeliveryInformationItemBinding

class DeliveryInformationAdapter(
    private val listener: OnDeliveryInformationListener
) : ListAdapter<DeliveryInformation, DeliveryInformationAdapter.DeliveryInformationViewHolder>(
    DELIVERY_INFO_COMPARATOR
) {

    companion object {
        private val DELIVERY_INFO_COMPARATOR =
            object : DiffUtil.ItemCallback<DeliveryInformation>() {
                override fun areItemsTheSame(
                    oldItem: DeliveryInformation,
                    newItem: DeliveryInformation
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: DeliveryInformation,
                    newItem: DeliveryInformation
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeliveryInformationViewHolder {
        val binding = DeliveryInformationItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return DeliveryInformationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeliveryInformationViewHolder, position: Int) {
        val item = getItem(position)

        if (item != null)
            holder.bind(item)
    }

    inner class DeliveryInformationViewHolder(private val binding: DeliveryInformationItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {

                btnDelete.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.onDeleteClicked(item)
                    }
                }

                btnMakeDefault.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.onMakeDefaultClicked(item)
                    }
                }

                btnEdit.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val item = getItem(position)
                        listener.onEditClicked(item)
                    }
                }
            }
        }

        fun bind(deliveryInfo: DeliveryInformation) {
            binding.apply {
                val contact = if (deliveryInfo.contactNo[0].toString() == "0")
                    deliveryInfo.contactNo.substring(
                        1,
                        deliveryInfo.contactNo.length - 1
                    ) else deliveryInfo.contactNo

                val nameAndContact = "${deliveryInfo.name} | (+63) $contact"
                tvNameAndContact.text = nameAndContact

                val completeAddress = "${deliveryInfo.streetNumber} " +
                    "${deliveryInfo.city}, " +
                    "${deliveryInfo.province}, " +
                    "${deliveryInfo.province}, " +
                    deliveryInfo.postalCode
                tvAddress.text = completeAddress
            }
        }
    }

    interface OnDeliveryInformationListener {
        fun onMakeDefaultClicked(deliveryInfo: DeliveryInformation)
        fun onEditClicked(deliveryInfo: DeliveryInformation)
        fun onDeleteClicked(deliveryInfo: DeliveryInformation)
    }
}
