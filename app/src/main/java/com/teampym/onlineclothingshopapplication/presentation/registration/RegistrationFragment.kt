package com.teampym.onlineclothingshopapplication.presentation.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_registration.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

const val ADD_EDIT_PROFILE_REQUEST = "add_edit_profile_request"
const val ADD_EDIT_PROFILE_RESULT = "add_edit_profile_result"

const val TAG = "RegistrationFragment"

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {

    private lateinit var binding: FragmentRegistrationBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel: RegistrationViewModel by viewModels()

    private val args by navArgs<RegistrationFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegistrationBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        FirebaseAuth.getInstance().currentUser?.let {
            Log.d(TAG, "auth: ${it.uid}")

            viewModel.userId = it.uid

            if (args.editMode) {
                loadingDialog.show()
            }

            viewModel.fetchUser(it.uid)
        }

        // Reuse this fragment when the user is editing his firstName, lastName, and birthDate
        val editMode = args.editMode

        viewModel.user.observe(viewLifecycleOwner) {
            loadingDialog.dismiss()

            if (it != null && editMode) {

                (requireActivity() as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.label_update_information)

                tvInstruction.isVisible = false
                btnRegister.text = getString(R.string.label_update_information)

                viewModel.firstName = it.firstName
                viewModel.lastName = it.lastName
                viewModel.birthDate = it.birthDate

                binding.apply {
                    edtFirstName.setText(viewModel.firstName)
                    edtLastName.setText(viewModel.lastName)
                    tvBirthdate.text = viewModel.birthDate
                }
            }
        }

        binding.apply {
            btnSelectDate.setOnClickListener {

                // Getting the date today
                val today = MaterialDatePicker.todayInUtcMilliseconds()
                // Setting the time TimeZone to UTC +0
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                calendar.timeInMillis = today
                calendar[Calendar.YEAR] = calendar[Calendar.YEAR] - 12
                calendar[Calendar.MONTH] = Calendar.DECEMBER
                val monthMinusTwelveYears = calendar.timeInMillis

                val constraintsBuilder =
                    CalendarConstraints.Builder()
                        .setOpenAt(monthMinusTwelveYears)
                        .setEnd(monthMinusTwelveYears)

                val birthDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select your birthdate")
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build()

                birthDatePicker.addOnPositiveButtonClickListener {
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
                    val selectedDate = Calendar.getInstance()
                    selectedDate.timeInMillis = it
                    viewModel.birthDate = formatter.format(selectedDate.time)
                    tvBirthdate.text = viewModel.birthDate
                }

                birthDatePicker.show(parentFragmentManager, "DatePicker")
            }

            btnRegister.setOnClickListener {
                submitForm()
            }

            edtFirstName.setText(viewModel.firstName)
            edtLastName.setText(viewModel.lastName)
            tvBirthdate.text = viewModel.birthDate

            edtFirstName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing to do here
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.firstName = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing to do here
                }
            })

            edtLastName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing to do here
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.lastName = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing to do here
                }
            })
        }

        lifecycleScope.launchWhenStarted {
            viewModel.registrationEvent.collectLatest { event ->
                when (event) {
                    is RegistrationViewModel.RegistrationEvent.ShowAddingSuccessAndNavigateBack -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        setFragmentResult(
                            ADD_EDIT_PROFILE_REQUEST,
                            bundleOf(ADD_EDIT_PROFILE_RESULT to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is RegistrationViewModel.RegistrationEvent.ShowUpdatingSuccessAndNavigateBack -> {
                        loadingDialog.dismiss()

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        setFragmentResult(
                            ADD_EDIT_PROFILE_REQUEST,
                            bundleOf(ADD_EDIT_PROFILE_RESULT to event.result)
                        )
                        findNavController().popBackStack()
                    }
                    is RegistrationViewModel.RegistrationEvent.ShowFormErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    is RegistrationViewModel.RegistrationEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun submitForm() {
        loadingDialog.show()
        viewModel.onSubmitClicked(
            args.editMode,
            FirebaseAuth.getInstance().currentUser?.let { it.photoUrl.toString() } ?: ""
        )
    }
}
