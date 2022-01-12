package com.teampym.onlineclothingshopapplication.presentation.faqs

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.FAQModel
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentFrequentlyAskQuestionsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "FrequentlyFragment"

@AndroidEntryPoint
class FrequentlyAskQuestionsFragment :
    Fragment(R.layout.fragment_frequently_ask_questions),
    FrequentlyAskQuestionsAdapter.FrequentlyAskQuestionsListener {

    private lateinit var binding: FragmentFrequentlyAskQuestionsBinding

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var adapter: FrequentlyAskQuestionsAdapter

    private val viewModel by viewModels<FrequentlyAskQuestionsViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFrequentlyAskQuestionsBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        setupViews()

        lifecycleScope.launchWhenStarted {
            viewModel.faqEvent.collectLatest { event ->
                when (event) {
                    is FrequentlyAskQuestionsViewModel.FAQEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is FrequentlyAskQuestionsViewModel.FAQEvent.ShowSuccessMessageAndNotifyAdapter -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.notifyItemRemoved(event.position)
                    }
                }
            }
        }
    }

    private fun setupViews() {
        adapter = FrequentlyAskQuestionsAdapter(this@FrequentlyAskQuestionsFragment, viewModel)

        viewModel.fetchFaqs()

        viewModel.faqs.observe(viewLifecycleOwner) {
            adapter.submitList(it)

            binding.apply {
                rvFaqs.setHasFixedSize(true)
                rvFaqs.layoutManager = LinearLayoutManager(requireContext())
                rvFaqs.adapter = adapter
            }
        }

        binding.apply {
            fabAdd.isVisible = viewModel.userType == UserType.ADMIN.name
            fabAdd.setOnClickListener {
                val action = FrequentlyAskQuestionsFragmentDirections
                    .actionFrequentlyAskQuestionsFragmentToAddEditFaqFragment()
                findNavController().navigate(action)
            }
        }

        setFragmentResultListener(ADD_EDIT_FAQ_REQUEST) { _, bundle ->
            val result = bundle.getString(ADD_EDIT_FAQ_RESULT)
            result?.let {
                Snackbar.make(
                    requireView(),
                    result,
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.fetchFaqs()
            }
        }
    }

    override fun onItemClicked(faq: FAQModel) {
        val action =
            FrequentlyAskQuestionsFragmentDirections.actionFrequentlyAskQuestionsFragmentToAddEditFaqFragment(
                faq
            )
        findNavController().navigate(action)
    }

    override fun onDeleteClicked(faq: FAQModel, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("DELETE FAQ")
            .setMessage("Are you sure you want to delete this question?")
            .setPositiveButton("Yes") { _, _ ->
                loadingDialog.show()
                viewModel.deleteFaq(faq, position)
            }.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }
}
