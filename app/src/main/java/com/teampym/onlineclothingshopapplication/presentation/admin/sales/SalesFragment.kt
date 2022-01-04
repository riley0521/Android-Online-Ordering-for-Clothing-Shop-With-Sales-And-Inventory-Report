package com.teampym.onlineclothingshopapplication.presentation.admin.sales

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.databinding.FragmentSalesBinding
import java.util.* // ktlint-disable no-wildcard-imports

private const val TAG = "SalesFragment"

class SalesFragment : Fragment(R.layout.fragment_sales) {

    private lateinit var binding: FragmentSalesBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSalesBinding.bind(view)

        val calendarDate = Calendar.getInstance()
        val month = calendarDate.get(Calendar.MONTH)
        Log.d(TAG, Utils.getCurrentMonth(month))
    }
}
