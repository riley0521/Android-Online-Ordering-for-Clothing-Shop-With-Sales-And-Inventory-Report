package com.teampym.onlineclothingshopapplication.presentation.client.others

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentTrackingNumberDialogBinding

const val TRACKING_NUMBER_REQUEST = "tracking_number_request"
const val TRACKING_NUMBER_RESULT = "tracking_number_result"

class TrackingNumberDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTrackingNumberDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tracking_number_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTrackingNumberDialogBinding.bind(view)

        binding.apply {
            etTrackingNumber.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    btnSubmit.isEnabled = s.toString().isNotBlank()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnSubmit.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("SUBMIT TRACKING NUMBER")
                    .setMessage("Are you sure that you typed the tracking number correctly? Please check again.")
                    .setPositiveButton("Yes") { _, _, ->
                        setFragmentResult(
                            TRACKING_NUMBER_REQUEST,
                            bundleOf(TRACKING_NUMBER_RESULT to etTrackingNumber.text.toString())
                        )
                        findNavController().popBackStack()
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }
    }
}
