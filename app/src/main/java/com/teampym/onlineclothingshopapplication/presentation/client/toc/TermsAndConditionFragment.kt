package com.teampym.onlineclothingshopapplication.presentation.client.toc

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentTermsAndConditionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TermsAndConditionFragment : Fragment(R.layout.fragment_terms_and_condition) {

    private lateinit var binding: FragmentTermsAndConditionBinding

    private val viewModel by viewModels<TermsAndConditionViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTermsAndConditionBinding.bind(view)

        viewModel.fetchTermsAndCondition()

        viewModel.termsAndCondition.observe(viewLifecycleOwner) {
            it?.let {
                binding.tvTermsAndCondition.text = it.tc
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tc_action_menu, menu)

        viewModel.userSession.observe(viewLifecycleOwner) {
            if (it.userType == UserType.ADMIN.name) {
                menu.findItem(R.id.action_edit_tc).isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit_tc -> {
                setFragmentResultListener(EDIT_TERMS_REQUEST) { _, bundle ->
                    val isSuccess = bundle.getBoolean(EDIT_TERMS_RESULT)
                    if (isSuccess) {
                        viewModel.fetchTermsAndCondition()
                    }
                }

                val action = TermsAndConditionFragmentDirections.actionTermsAndConditionFragmentToEditTermsAndConditionFragment(
                    viewModel.termsAndCondition.value!!
                )
                findNavController().navigate(action)
                true
            }
            else -> false
        }
    }
}
