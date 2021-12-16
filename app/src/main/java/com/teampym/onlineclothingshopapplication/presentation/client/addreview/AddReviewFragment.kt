package com.teampym.onlineclothingshopapplication.presentation.client.addreview

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddReviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddReviewFragment : Fragment(R.layout.fragment_add_review) {

    private lateinit var binding: FragmentAddReviewBinding

    private val viewModel by viewModels<AddReviewViewModel>()

    private val args by navArgs<AddReviewFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddReviewBinding.bind(view)

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

            productRating.setOnRatingBarChangeListener { _, rating, _ ->
                viewModel.ratingValue = rating
            }

            Glide.with(requireView())
                .load(orderDetail.product.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(imgProduct)

            btnSubmit.setOnClickListener {
                if (viewModel.ratingValue > 0) {
                    viewModel.onSubmitClicked(orderDetail, args.userInfo)
                    Toast.makeText(
                        requireContext(),
                        "Thanks for adding review to the product.",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                } else {
                    Snackbar.make(
                        requireView(),
                        "Please add at least 1 star rating",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
