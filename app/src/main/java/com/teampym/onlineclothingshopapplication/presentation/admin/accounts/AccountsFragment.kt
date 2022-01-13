package com.teampym.onlineclothingshopapplication.presentation.admin.accounts

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAccountsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountsFragment :
    Fragment(R.layout.fragment_accounts),
    AccountPagingAdapter.AccountListener {

    private lateinit var binding: FragmentAccountsBinding

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var adapter: AccountPagingAdapter

    private val viewModel by viewModels<AccountsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAccountsBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = AccountPagingAdapter(this, requireContext())

        binding.apply {
            rvAccounts.setHasFixedSize(true)
            rvAccounts.layoutManager = LinearLayoutManager(requireContext())
            rvAccounts.adapter = adapter

            refreshLayout.setOnRefreshListener {
                adapter.refresh()
            }
        }

        lifecycleScope.launchWhenStarted {
            launch {
                viewModel.accounts.collectLatest {
                    binding.refreshLayout.isRefreshing = false

                    adapter.submitData(it)
                }
            }

            launch {
                viewModel.accountsEvent.collectLatest { event ->
                    when (event) {
                        is AccountsViewModel.AccountsEvent.ShowErrorMessage -> {
                            loadingDialog.dismiss()
                            Snackbar.make(
                                requireView(),
                                event.msg,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        is AccountsViewModel.AccountsEvent.ShowSuccessMessage -> {
                            loadingDialog.dismiss()
                            Snackbar.make(
                                requireView(),
                                event.msg,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            adapter.refresh()
                            binding.refreshLayout.isRefreshing = true
                        }
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.account_action_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value
        if (pendingQuery != null && pendingQuery.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery, false)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Nothing to do here
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Search
                viewModel.searchQuery.value = newText
                return true
            }
        })

        searchView.setOnCloseListener {
            viewModel.searchQuery.value = ""
            true
        }
    }

    override fun onBanClicked(user: UserInformation) {
        loadingDialog.show()
        viewModel.banUser(user)
    }

    override fun onUnBanClicked(user: UserInformation) {
        loadingDialog.show()
        viewModel.unBanUser(user)
    }
}
