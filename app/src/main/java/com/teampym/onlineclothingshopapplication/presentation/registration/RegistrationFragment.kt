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
import com.teampym.onlineclothingshopapplication.data.models.Utils
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegistrationBinding.bind(view)

        // Reuse this fragment when the user is editing his firstName, lastName, and birthDate
        val editMode = args.editMode

        binding.apply {

            if (editMode) {
                tvInstruction.isVisible = false
                btnRegister.text = "Update Information"

                edtFirstName.text.apply { Utils.currentUser?.firstName }
                edtFirstName.text.apply { Utils.currentUser?.lastName }
                tvBirthdate.text = Utils.currentUser?.birthDate
            }

            btnSelectDate.setOnClickListener {
                // TODO("Show material date picker and get selected date and display it to tvBirthdate textview")

                val today = MaterialDatePicker.todayInUtcMilliseconds()
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

                calendar.timeInMillis = today
                calendar[Calendar.YEAR].minus(12)
                val monthMinusTwelveYears = calendar.timeInMillis

                val constraintsBuilder =
                    CalendarConstraints.Builder()
                        .setOpenAt(monthMinusTwelveYears)
                        .setEnd(monthMinusTwelveYears)

                val birthDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Your Birthdate")
                    .setSelection(monthMinusTwelveYears)
                    .setCalendarConstraints(constraintsBuilder.build())
                    .build()

                birthDatePicker.addOnPositiveButtonClickListener {
                    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
                    val selectedDate = Calendar.getInstance()
                    selectedDate.timeInMillis = it
                    selectedBirthDate = formatter.format(selectedDate.time)
                }

                birthDatePicker.show(parentFragmentManager, "DatePicker")
            }

            btnRegister.setOnClickListener {
                if (editMode) {

                } else {
                    viewModel.registerUser(
                        edtFirstName.text.toString(),
                        edtLastName.text.toString(),
                        selectedBirthDate,
                        FirebaseAuth.getInstance().currentUser!!
                    )
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

        viewModel.createdUser.observe(viewLifecycleOwner) {
            if (it.userId == "failed")
                Snackbar.make(requireView(), "Registration failed", Snackbar.LENGTH_LONG).show()
            else
                viewModel.saveInfoToUtils(it)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.registrationEvent.collect { event ->
                when (event) {
                    is RegistrationViewModel.RegistrationEvent.SuccessfulEvent -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_registrationFragment_to_categoryFragment)
                    }
                }
            }
        }
    }

    fun checkDataIfValid(editMode: Boolean): Boolean {
        if (editMode) {
            return !Utils.currentUser?.firstName!!.equals(edtFirstName.text.toString(), false) or
                    !Utils.currentUser?.lastName!!.equals(edtLastName.text.toString(), false)
        }
        return edtFirstName.text!!.isNotBlank() && edtLastName.text!!.isNotBlank() && selectedBirthDate.isNotBlank()
    }
}