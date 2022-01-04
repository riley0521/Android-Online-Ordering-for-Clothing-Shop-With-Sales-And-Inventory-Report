package com.teampym.onlineclothingshopapplication.presentation.admin.audit_history

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.AuditType
import com.teampym.onlineclothingshopapplication.databinding.FragmentHistoryLogBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class HistoryLogFragment : Fragment(R.layout.fragment_history_log) {

    private lateinit var binding: FragmentHistoryLogBinding

    private lateinit var adapter: HistoryLogAdapter

    private val viewModel by viewModels<HistoryLogViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHistoryLogBinding.bind(view)

        adapter = HistoryLogAdapter(requireContext())

        binding.apply {
            rvHistoryLogs.setHasFixedSize(true)
            rvHistoryLogs.layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            rvHistoryLogs.adapter = adapter
        }

        lifecycleScope.launchWhenStarted {
            viewModel.historyLogs.collectLatest {
                (requireActivity() as AppCompatActivity).supportActionBar?.title = getString(
                    R.string.placeholder_history_log_title,
                    viewModel.filterType
                )

                adapter.submitData(it)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history_log_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.by_category -> {
                viewModel.updateFilterLogType(AuditType.CATEGORY)
                true
            }
            R.id.by_product -> {
                viewModel.updateFilterLogType(AuditType.PRODUCT)
                true
            }
            R.id.by_inventory -> {
                viewModel.updateFilterLogType(AuditType.INVENTORY)
                true
            }
            R.id.by_order -> {
                viewModel.updateFilterLogType(AuditType.ORDER)
                true
            }
            else -> false
        }
    }
}
