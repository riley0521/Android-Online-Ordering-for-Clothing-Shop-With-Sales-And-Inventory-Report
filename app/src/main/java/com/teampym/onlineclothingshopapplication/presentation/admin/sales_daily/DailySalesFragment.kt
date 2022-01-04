package com.teampym.onlineclothingshopapplication.presentation.admin.sales_daily

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentDailySalesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DailySalesFragment : Fragment(R.layout.fragment_daily_sales) {

    private lateinit var binding: FragmentDailySalesBinding

    private lateinit var adapter: DailySalesAdapter

    private val args by navArgs<DailySalesFragmentArgs>()

    private val viewModel by viewModels<DailySalesViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentDailySalesBinding.bind(view)

        adapter = DailySalesAdapter(requireContext())

        viewModel.fetchDailySales(args.year, args.month)

        binding.apply {
            rvDailySales.setHasFixedSize(true)
            rvDailySales.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            rvDailySales.adapter = adapter
        }

        viewModel.dailySales.observe(viewLifecycleOwner) { dayList ->
            adapter.submitList(dayList.sortedBy { it.id })
        }
    }
}
