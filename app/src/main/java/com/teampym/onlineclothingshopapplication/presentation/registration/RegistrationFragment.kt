package com.teampym.onlineclothingshopapplication.presentation.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_registration.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.* // ktlint-disable no-wildcard-imports

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {

    private lateinit var binding: FragmentRegistrationBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel: RegistrationViewModel by viewModels()

    private val args by navArgs<RegistrationFragmentArgs>()

    private var userInfo: UserInformation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegistrationBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())
        loadingDialog.show()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.userId = currentUser.uid
            viewModel.fetchNotificationTokensAndWishList(currentUser.uid)
        }

        // Reuse this fragment when the user is editing his firstName, lastName, and birthDate
        val editMode = args.editMode
        if (!editMode) {
            if (loadingDialog.isActive()) {
                loadingDialog.dismiss()
            }
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null && editMode) {
                if (loadingDialog.isActive()) {
                    loadingDialog.dismiss()
                }

                tvInstruction.isVisible = false
                btnRegister.text = getString(R.string.label_update_information)

                userInfo = it
                binding.edtFirstName.text.apply { it.firstName }
                binding.edtLastName.text.apply { it.lastName }
                binding.tvBirthdate.text = it.birthDate
            }
        }

        binding.apply {
            btnSelectDate.setOnClickListener {
                // TODO("Show material date picker and get selected date and display it to tvBirthdate textview")

                val today = MaterialDatePicker.todayInUtcMilliseconds()
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
                viewModel.onSubmitClicked(
                    editMode,
                    currentUser?.photoUrl.toString()
                )
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
                    is RegistrationViewModel.RegistrationEvent.ShowSuccessfulMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }
                    is RegistrationViewModel.RegistrationEvent.ShowErrorMessage -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}
