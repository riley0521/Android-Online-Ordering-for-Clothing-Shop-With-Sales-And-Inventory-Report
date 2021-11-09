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
import com.teampym.onlineclothingshopapplication.databinding.FragmentRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_registration.*
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {

    private lateinit var binding: FragmentRegistrationBinding

    private val viewModel: RegistrationViewModel by viewModels()

    private val args by navArgs<RegistrationFragmentArgs>()

    private var selectedBirthDate: String = ""

    private var userInfo: UserInformation? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegistrationBinding.bind(view)

        val currentUser = FirebaseAuth.getInstance().currentUser

        // Reuse this fragment when the user is editing his firstName, lastName, and birthDate
        val editMode = args.editMode

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
                    selectedBirthDate = formatter.format(selectedDate.time)
                    tvBirthdate.text = selectedBirthDate
                    btnRegister.isEnabled = checkDataIfValid(editMode)
                }

                birthDatePicker.show(parentFragmentManager, "DatePicker")
            }

            btnRegister.setOnClickListener {
                if (editMode && currentUser != null) {
                    viewModel.updateBasicInformation(
                        edtFirstName.text.toString(),
                        edtLastName.text.toString(),
                        selectedBirthDate,
                        currentUser.uid
                    )
                    return@setOnClickListener
                }
                else {
                    if (currentUser != null) {
                        viewModel.registerUser(
                            edtFirstName.text.toString(),
                            edtLastName.text.toString(),
                            selectedBirthDate,
                            currentUser
                        )
                    }
                }
            }

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
                    btnRegister.isEnabled = checkDataIfValid(editMode)
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
                    btnRegister.isEnabled = checkDataIfValid(editMode)
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing to do here
                }
            })

        }

        viewModel.user.observe(viewLifecycleOwner) {
            if(it != null && editMode) {

                tvInstruction.isVisible = false
                btnRegister.text = "Update Information"

                userInfo = it
                binding.edtFirstName.text.apply { it.firstName }
                binding.edtLastName.text.apply { it.lastName }
                binding.tvBirthdate.text = it.birthDate
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.registrationEvent.collect { event ->
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

    fun checkDataIfValid(editMode: Boolean): Boolean {
        if (editMode) {
            return !userInfo!!.firstName.equals(edtFirstName.text.toString(), true) or
                    !userInfo!!.lastName.equals(edtLastName.text.toString(), true)
        }
        return edtFirstName.text!!.isNotBlank() && edtLastName.text!!.isNotBlank() && selectedBirthDate.isNotBlank()
    }
}