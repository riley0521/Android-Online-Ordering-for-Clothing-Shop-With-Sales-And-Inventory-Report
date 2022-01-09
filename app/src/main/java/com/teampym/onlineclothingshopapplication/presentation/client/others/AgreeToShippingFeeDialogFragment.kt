package com.teampym.onlineclothingshopapplication.presentation.client.others

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAgreeToShippingFeeDialogBinding
import dagger.hilt.android.AndroidEntryPoint

const val AGREE_TO_SF_REQUEST = "agree_to_sf_request"
const val AGREE_TO_SF_RESULT = "agree_to_sf_result"

@AndroidEntryPoint
class AgreeToShippingFeeDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAgreeToShippingFeeDialogBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<AgreeToShippingFeeDialogFragmentArgs>()

    private val viewModel by viewModels<AgreeToShippingFeeViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agree_to_shipping_fee_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAgreeToShippingFeeDialogBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val order = args.order

        binding.apply {
            btnAgree.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("AGREE TO SUGGESTED SHIPPING FEE")
                    .setMessage("Are you sure you want to agree? You cannot reverse this action.")
                    .setPositiveButton("Yes") { _, _ ->
                        loadingDialog.show()
                        viewModel.agreeToSf(order)
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }

            btnDisagree.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("DISAGREE TO SUGGESTED SHIPPING FEE")
                    .setMessage("Are you sure you want to disagree? This will cancel your order right away.")
                    .setPositiveButton("Yes") { _, _ ->
                        loadingDialog.show()
                        viewModel.cancelOrder(order)
                    }.setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }.show()
            }
        }

        viewModel.hasAgreed.observe(viewLifecycleOwner) {
            if (it) {
                loadingDialog.dismiss()

                // Go back to the parent fragment
                setFragmentResult(
                    AGREE_TO_SF_REQUEST,
                    bundleOf(AGREE_TO_SF_REQUEST to "Agreed to suggested shipping fee successfully!")
                )
                findNavController().popBackStack()
            }
        }

        viewModel.isCanceled.observe(viewLifecycleOwner) {
            if (it) {
                loadingDialog.dismiss()

                // Go back to the parent fragment
                setFragmentResult(
                    AGREE_TO_SF_REQUEST,
                    bundleOf(AGREE_TO_SF_REQUEST to "Disagreed to suggested shipping fee and canceled the order.")
                )
                findNavController().popBackStack()
            }
        }
    }
}
