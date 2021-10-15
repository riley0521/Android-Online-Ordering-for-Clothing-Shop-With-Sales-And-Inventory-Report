package com.teampym.onlineclothingshopapplication.presentation.registration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentRegistrationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_registration.*
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class RegistrationFragment : Fragment(R.layout.fragment_registration) {

    private lateinit var binding: FragmentRegistrationBinding

    private val viewModel: RegistrationViewModel by viewModels()

    private val args by navArgs<RegistrationFragmentArgs>()

    private val selectedBirthDate: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegistrationBinding.bind(view)

        binding.apply {
            btnRegister.setOnClickListener {
                viewModel.registerUser(
                    edtFirstName.text.toString(),
                    edtLastName.text.toString(),
                    selectedBirthDate,
                    FirebaseAuth.getInstance().currentUser!!
                )
            }

            edtFirstName.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing to do here
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    btnRegister.isEnabled = checkDataIfValid()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing to do here
                }
            })

            edtLastName.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing to do here
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    btnRegister.isEnabled = checkDataIfValid()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing to do here
                }
            })

        }

        viewModel.createdUser.observe(viewLifecycleOwner) {
            if(it.id == "failed")
                Snackbar.make(requireView(), "Registration failed", Snackbar.LENGTH_LONG).show()
            else
                viewModel.saveInfoToProto(it)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.registrationEvent.collect { event ->
                when(event) {
                    is RegistrationViewModel.RegistrationEvent.SuccessfulEvent -> {
                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_registrationFragment_to_categoryFragment)
                    }
                }
            }
        }
    }

    fun checkDataIfValid() = edtFirstName.text!!.isNotBlank() && edtLastName.text!!.isNotBlank() && selectedBirthDate.isNotBlank()
}