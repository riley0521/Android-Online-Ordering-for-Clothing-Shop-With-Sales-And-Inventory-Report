package com.teampym.onlineclothingshopapplication.presentation.client.orderlist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentOrderListBinding

class OrderListFragment : Fragment(R.layout.fragment_order_list) {

    private lateinit var binding: FragmentOrderListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderListBinding.bind(view)
    }
}
