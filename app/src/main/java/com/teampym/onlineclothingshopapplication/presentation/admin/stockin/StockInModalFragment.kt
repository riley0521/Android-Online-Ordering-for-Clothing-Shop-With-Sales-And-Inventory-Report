package com.teampym.onlineclothingshopapplication.presentation.admin.stockin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentStockInModalBinding

class StockInModalFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentStockInModalBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stock_in_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentStockInModalBinding.bind(view)
    }
}
