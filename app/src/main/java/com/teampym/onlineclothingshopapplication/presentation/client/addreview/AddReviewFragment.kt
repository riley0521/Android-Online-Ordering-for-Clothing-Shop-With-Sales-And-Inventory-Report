package com.teampym.onlineclothingshopapplication.presentation.client.addreview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RatingBar
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddReviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

const val ADD_REVIEW_REQUEST = "add_review_request"
const val ADD_REVIEW_RESULT = "add_review_result"

@AndroidEntryPoint
class AddReviewFragment : Fragment(R.layout.fragment_add_review),
    RatingBar.OnRatingBarChangeListener {

    private lateinit var binding: FragmentAddReviewBinding

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<AddReviewViewModel>()

    private val args by navArgs<AddReviewFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddReviewBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val orderDetail = args.orderDetail

        binding.apply {
            productRating.rating = viewModel.ratingValue
            etFeedback.setText(viewModel.feedbackValue)

            etFeedback.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.feedbackValue = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            productRating.onRatingBarChangeListener = this@AddReviewFragment

            Glide.with(requireView())
                .load(orderDetail.product.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(imgProduct)

            btnSubmit.setOnClickListener {
                if (viewModel.ratingValue > 0) {
                    loadingDialog.show()

                    viewModel.onSubmitClicked(orderDetail, args.userInfo)
                    Snackbar.make(
                        requireView(),
                        "Submitting review...",
                        Snackbar.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        requireView(),
                        "Please add at least 1 star rating",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.addReviewEvent.collectLatest { event ->
                when (event) {
                    is AddReviewViewModel.AddReviewEvent.NavigateBackWithResult -> {
                        loadingDialog.dismiss()

                        setFragmentResult(
                            ADD_REVIEW_REQUEST,
                            bundleOf(ADD_REVIEW_RESULT to event.isSuccess)
                        )
                        findNavController().popBackStack()
                    }
                    is AddReviewViewModel.AddReviewEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onRatingChanged(ratingBar: RatingBar?, rating: Float, fromUser: Boolean) {
        viewModel.ratingValue = rating
    }
}
