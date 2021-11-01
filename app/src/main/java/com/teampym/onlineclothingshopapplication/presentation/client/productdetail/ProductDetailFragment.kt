package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import android.os.Bundle
import android.text.Layout
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.ProductImage
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.product_item.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    private lateinit var binding: FragmentProductDetailBinding

    private val args by navArgs<ProductDetailFragmentArgs>()

    private val viewModel: ProductDetailViewModel by viewModels()

    @Inject
    lateinit var db: FirebaseFirestore

    private lateinit var adapter: ReviewAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductDetailBinding.bind(view)

        val product = args.product

        adapter = ReviewAdapter()

        binding.btnAddToCart.setOnClickListener {
            val action = ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(product)
            findNavController().navigate(action)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.provideQuery(product.id)

            viewModel.reviewsFlow.collect {
                adapter.submitData(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            val queryReviews = db.collection(PRODUCTS_COLLECTION)
                .document(product.id)
                .collection(REVIEWS_SUB_COLLECTION)
                .get()
                .await()

            var rate = 0.0

            if (queryReviews.size() > 0)
                for (review in queryReviews.documents) {
                    rate += review.getDouble("rate")!!
                }

            binding.apply {

                Glide.with(requireView())
                    .load(product.imageUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_food)
                    .into(imgProduct)

                tvProductName.text = product.name
                tvPrice.text = "$${product.price}"
                tvDescription.text = product.description
                btnAddToCart.setOnClickListener {
                    val action = ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(product)
                    findNavController().navigate(action)
                }

                if (rate == 0.0) {
                    tvRate.text = rate.toString()

                    labelNoReviews.isVisible = true
                } else {
                    tvRate.text = rate.toString()

                    // Load Reviews here.
                    recyclerReviews.setHasFixedSize(true)
                    recyclerReviews.adapter = adapter
                }
            }
        }
    }

}