package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    private lateinit var binding: FragmentProductDetailBinding

    private val args by navArgs<ProductDetailFragmentArgs>()

    private val viewModel: ProductDetailViewModel by viewModels()

    @Inject
    lateinit var db: FirebaseFirestore

    private lateinit var adapter: ReviewAdapter

    private var myMenu: Menu? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductDetailBinding.bind(view)
        adapter = ReviewAdapter()

        var product = args.product
        val productId = args.productId

        if (product == null) {
            viewModel.getProductById(productId!!)
        }

        viewModel.product.observe(viewLifecycleOwner) {
            product = it
        }

        binding.apply {
            btnAddToCart.setOnClickListener {
                val action =
                    ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(
                        product!!
                    )
                findNavController().navigate(action)
            }

            Glide.with(requireView())
                .load(product?.imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.ic_food)
                .into(imgProduct)

            val priceStr = "$" + product?.price
            tvProductName.text = product?.name
            tvPrice.text = priceStr
            val descStr = product?.description ?: "No Available Description"

            tvDescription.text = descStr
            btnAddToCart.setOnClickListener {
                val action =
                    ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(
                        product!!
                    )
                findNavController().navigate(action)
            }

            // submit list to the adapter if the reviewList is not empty.
            product.let { p ->
                adapter.submitList(p?.reviewList)

                var rate = 0.0
                if (p?.totalRate!! > 0.0 || p.numberOfReviews > 0L) {
                    rate = p.avgRate
                }

                if (rate == 0.0) {
                    labelRate.text = getString(R.string.no_available_rating)

                    tvRate.visibility = View.INVISIBLE
                    labelNoReviews.isVisible = true
                } else {
                    val rateStr = "- $rate"
                    tvRate.text = rateStr

                    // Load Reviews here.
                    recyclerReviews.setHasFixedSize(true)
                    recyclerReviews.adapter = adapter
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.userFlow.collectLatest { user ->
                if (user.userType == UserType.CUSTOMER.name) {
                    myMenu?.let {
                        it.findItem(R.id.action_add_edit_stock).isVisible = false
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_detail_action_menu, menu)

        myMenu = menu
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                // TODO("Create copy link")
                true
            }
            R.id.action_cart -> {
                findNavController().navigate(R.id.action_global_cartFragment)
                true
            }
            R.id.action_edit_product -> {
                // TODO("Navigate to add/edit product layout when admin")
                true
            }
            R.id.action_add_new_inventory -> {
                // TODO("Navigate to add inventory layout when admin")
                true
            }
            R.id.action_stock_in -> {
                // TODO("Navigate to stock in layout when admin")
                true
            }
            else -> false
        }
    }
}
