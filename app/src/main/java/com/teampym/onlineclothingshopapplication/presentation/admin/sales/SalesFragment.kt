package com.teampym.onlineclothingshopapplication.presentation.admin.sales

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.YearSale
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.databinding.FragmentSalesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.* // ktlint-disable no-wildcard-imports

private const val TAG = "SalesFragment"

@AndroidEntryPoint
class SalesFragment : Fragment(R.layout.fragment_sales), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentSalesBinding
    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<SalesViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSalesBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())
        loadingDialog.show()

        val calendarDate = Calendar.getInstance()
        calendarDate.timeInMillis = Utils.getTimeInMillisUTC()

        val year = calendarDate.get(Calendar.YEAR).toString()
        val monthIndex = calendarDate.get(Calendar.MONTH)
        val month = Utils.getCurrentMonth(monthIndex)
        val day = calendarDate.get(Calendar.DAY_OF_MONTH)
        Log.d(TAG, "$year, $month - $day")

        if (viewModel.isFirstTime) {
            viewModel.updateYear(year)
            viewModel.updateMonth(month)

            viewModel.isFirstTime = false
        }

        lifecycleScope.launchWhenStarted {
            viewModel.salesForSelectedMonthAndYear.collectLatest { yearSale ->
                loadingDialog.dismiss()

                if (yearSale != null) {
                    setupViews(yearSale)
                } else {
                    withContext(Dispatchers.Main) {
                        setupSpinners()
                        binding.btnViewSalesDaily.setOnClickListener {
                            Snackbar.make(
                                requireView(),
                                "There is no sales for this month yet.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupViews(yearSale: YearSale) = CoroutineScope(Dispatchers.Main).launch {
        binding.apply {
            setupSpinners()

            val selectedMonth = yearSale.listOfMonth.firstOrNull { it.id == viewModel.month.value }

            tvSelectedYear.text = getString(
                R.string.placeholder_total_sale_for_year,
                yearSale.id
            )

            tvSelectedMonth.text = getString(
                R.string.placeholder_total_sale_for_month_of,
                selectedMonth?.id
            )

            val yearSaleStr = "₱" + yearSale.totalSale
            tvYearlySale.text = yearSaleStr

            val monthSaleStr = "₱${selectedMonth?.totalSale}"
            tvMonthlySale.text = monthSaleStr

            btnViewSalesDaily.setOnClickListener {
                if (selectedMonth?.totalSale ?: 0.0 > 0.0 && yearSale.listOfMonth.isNotEmpty()) {
                    val action = SalesFragmentDirections.actionSalesFragmentToDailySalesFragment(
                        viewModel.year.value!!,
                        viewModel.month.value!!
                    )
                    findNavController().navigate(action)
                } else {
                    Snackbar.make(
                        requireView(),
                        "There is no sales for this month yet.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupSpinners() {
        binding.apply {
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Utils.getAvailableYears()
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spYear.adapter = adapter

                spYear.setSelection(adapter.getPosition(viewModel.year.value))
            }

            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Utils.getMonthNames()
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spMonth.adapter = adapter

                spMonth.setSelection(adapter.getPosition(viewModel.month.value))
            }

            spYear.onItemSelectedListener = this@SalesFragment
            spMonth.onItemSelectedListener = this@SalesFragment
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            binding.spYear.id -> {
                viewModel.updateYear(Utils.getAvailableYears()[position])
            }
            binding.spMonth.id -> {
                viewModel.updateMonth(Utils.getCurrentMonth(position))
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Nothing to do here...
    }
}
