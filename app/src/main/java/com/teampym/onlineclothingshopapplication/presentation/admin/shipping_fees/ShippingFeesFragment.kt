package com.teampym.onlineclothingshopapplication.presentation.admin.shipping_fees

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentShippingFeesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "SFragment"

@AndroidEntryPoint
class ShippingFeesFragment : Fragment(R.layout.fragment_shipping_fees) {

    private lateinit var binding: FragmentShippingFeesBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<ShippingFeesViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShippingFeesBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        loadingDialog.show()

        binding.apply {
            viewModel.loadShippingFees {
                loadingDialog.dismiss()

                viewModel.shippingFee = it
                Log.d(TAG, "onViewCreated: $it")

                etMetroManila.setText(viewModel.shippingFee.metroManila.toString())
                etMindanao.setText(viewModel.shippingFee.mindanao.toString())
                etNorthLuzon.setText(viewModel.shippingFee.northLuzon.toString())
                etSouthLuzon.setText(viewModel.shippingFee.southLuzon.toString())
                etVisayas.setText(viewModel.shippingFee.visayas.toString())

                etMetroManila.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // Nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.toString().isNotBlank()) {
                            viewModel.shippingFee.metroManila = s.toString().toInt()
                        } else {
                            viewModel.shippingFee.metroManila = 0
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Nothing
                    }
                })

                etMindanao.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // Nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.toString().isNotBlank()) {
                            viewModel.shippingFee.mindanao = s.toString().toInt()
                        } else {
                            viewModel.shippingFee.mindanao = 0
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Nothing
                    }
                })

                etNorthLuzon.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // Nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.toString().isNotBlank()) {
                            viewModel.shippingFee.northLuzon = s.toString().toInt()
                        } else {
                            viewModel.shippingFee.northLuzon = 0
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Nothing
                    }
                })

                etSouthLuzon.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // Nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.toString().isNotBlank()) {
                            viewModel.shippingFee.southLuzon = s.toString().toInt()
                        } else {
                            viewModel.shippingFee.southLuzon = 0
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Nothing
                    }
                })

                etVisayas.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // Nothing
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (s.toString().isNotBlank()) {
                            viewModel.shippingFee.visayas = s.toString().toInt()
                        } else {
                            viewModel.shippingFee.visayas = 0
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {
                        // Nothing
                    }
                })

                btnSubmit.setOnClickListener {
                    loadingDialog.show()
                    viewModel.onSubmitClicked()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.shippingFeesEvent.collectLatest { event ->
                when (event) {
                    is ShippingFeesViewModel.ShippingFeesEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is ShippingFeesViewModel.ShippingFeesEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()

                        Toast.makeText(
                            requireContext(),
                            event.msg,
                            Toast.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}
