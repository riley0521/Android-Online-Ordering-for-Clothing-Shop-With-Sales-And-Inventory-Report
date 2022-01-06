package com.teampym.onlineclothingshopapplication.presentation.client.productdetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.room.Product
import com.teampym.onlineclothingshopapplication.data.util.LinkType
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.PREFIX
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.data.util.Utils
import com.teampym.onlineclothingshopapplication.data.util.shareDeepLink
import com.teampym.onlineclothingshopapplication.databinding.FragmentProductDetailBinding
import com.teampym.onlineclothingshopapplication.presentation.admin.stockin.STOCK_IN_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.admin.stockin.STOCK_IN_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ProductDetailFragment : Fragment(R.layout.fragment_product_detail) {

    private lateinit var binding: FragmentProductDetailBinding

    private lateinit var loadingDialog: LoadingDialog

    private val args by navArgs<ProductDetailFragmentArgs>()

    private val viewModel: ProductDetailViewModel by viewModels()

    private lateinit var adapter: ReviewAdapter

    private var myMenu: Menu? = null

    private var userId = ""

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProductDetailBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        adapter = ReviewAdapter()

        if (args.product == null) {
            // Product ID Should not be null if Product Parcelable is null
            viewModel.getProductById(args.productId!!)
        } else {
            viewModel.updateProduct(args.product!!)
        }

        // Re-assign product variable in-case if it's null and to survive process death
        viewModel.product.observe(viewLifecycleOwner) { product ->
            product?.let { p ->
                (requireActivity() as AppCompatActivity).supportActionBar?.title = p.name

                setupViews(p)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.productDetailEvent.collectLatest { event ->
                when (event) {
                    is ProductDetailViewModel.ProductDetailEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is ProductDetailViewModel.ProductDetailEvent.ShowSuccessMessage -> {
                        requireActivity().invalidateOptionsMenu()
                        delay(1000)
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

        setHasOptionsMenu(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupViews(product: Product) {
        binding.apply {
            btnAddToCart.setOnClickListener {
                val action =
                    ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(
                        product
                    )
                findNavController().navigate(action)
            }

            val priceStr = "$" + product.price
            tvProductName.text = product.name
            tvPrice.text = priceStr
            val descStr = if (product.description.isBlank()) {
                "No Available Description"
            } else {
                product.description
            }

            tvDescription.text = descStr
            btnAddToCart.setOnClickListener {
                val action =
                    ProductDetailFragmentDirections.actionProductDetailFragmentToInventoryModalFragment(
                        product
                    )
                findNavController().navigate(action)
            }

            // submit list to the image adapter
            val viewPager = carouselViewPager.apply {
                adapter = ImagePagerAdapter(requireActivity()).apply {
                    submitList(product.productImageList)
                    notifyDataSetChanged()
                }
            }

            // Attach viewPager and the tabLayout together so that they can work altogether
            TabLayoutMediator(indicatorTabLayout, viewPager) { _, _ -> }.attach()

            var rate = 0.0
            if (product.totalRate > 0.0 && product.numberOfReviews > 0L) {
                rate = product.avgRate.toDouble()
            }

            val rateStr = "- $rate"
            tvRate.text = rateStr
            ratingBar.rating = rate.toFloat()

            if (product.numberOfReviews > 5L) {
                tvShowMoreReviews.text =
                    getString(R.string.label_show_more_reviews, product.numberOfReviews)

                // Navigate the user to show all reviews of this product
                // To let them know if the product is worth buying.
                tvShowMoreReviews.setOnClickListener {
                    val action =
                        ProductDetailFragmentDirections.actionProductDetailFragmentToReviewsFragment(
                            productId = product.productId
                        )
                    findNavController().navigate(action)
                }
            } else {
                tvShowMoreReviews.isVisible = false
            }

            if (rate == 0.0) {
                labelNoReviews.isVisible = true
            } else {
                // submit list to the adapter if the reviewList is not empty.
                adapter.submitList(product.reviewList)

                // Show the items in the adapter in this recyclerViewReviews.
                recyclerReviews.setHasFixedSize(true)
                recyclerReviews.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()

        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.product_detail_action_menu, menu)

        myMenu = menu

        viewModel.getUserSession().observe(viewLifecycleOwner) { session ->
            if (session.userId.isNotBlank()) {
                userId = session.userId
            }

            when (session.userType) {
                UserType.CUSTOMER.name -> {
                    myMenu?.let { menu ->
                        menu.findItem(R.id.action_add_to_wishlist).isVisible = true
                        menu.findItem(R.id.action_cart).isVisible = true

                        viewModel.product.value?.let { p ->
                            viewModel.checkIfProductExistInWishList(p.productId)
                        }

                        viewModel.isExisting.observe(viewLifecycleOwner) {
                            if (it) {
                                menu.findItem(R.id.action_add_to_wishlist)
                                    .setIcon(R.drawable.ic_fav_checked)
                            } else {
                                menu.findItem(R.id.action_add_to_wishlist)
                                    .setIcon(R.drawable.ic_fav_unchecked)
                            }
                        }
                    }
                }
                UserType.ADMIN.name -> {
                    myMenu?.let {
                        it.findItem(R.id.action_add_edit_stock).isVisible = true
                        binding.btnAddToCart.isVisible = false
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {

                // Create a shareable link that will be interpreted by messenger
                // WhatsApp, Telegram, Discord, etc...
                viewModel.product.value?.let { p ->
                    Utils.generateSharingLink(
                        p.name,
                        "$PREFIX/product/${p.productId}".toUri(),
                        p.imageUrl.toUri()
                    ) {

                        // It means that the creation of shortDynamicLink was successful
                        if (it != "None") {
                            this@ProductDetailFragment.shareDeepLink(
                                it,
                                LinkType.PRODUCT
                            )
                        }
                    }
                }
                true
            }
            R.id.action_cart -> {
                findNavController().navigate(R.id.action_global_cartFragment)
                true
            }
            R.id.action_edit_product -> {
                // Navigate to add/edit product layout when admin
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToAddEditProductFragment(
                        "Edit Product (${viewModel.product.value!!.productId})",
                        viewModel.product.value,
                        true,
                        viewModel.product.value!!.categoryId
                    )
                findNavController().navigate(action)
                true
            }
            R.id.action_add_new_inventory -> {
                // Navigate to add inventory layout when admin
                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToAddInventoryFragment(
                        productId = viewModel.product.value!!.productId,
                        productName = viewModel.product.value!!.name
                    )
                findNavController().navigate(action)
                true
            }
            R.id.action_stock_in -> {
                setFragmentResultListener(STOCK_IN_REQUEST) { _, bundle ->
                    val result = bundle.getBoolean(STOCK_IN_RESULT)
                    if (result) {
                        Snackbar.make(
                            requireView(),
                            "Stock added successfully.",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        viewModel.getProductById(args.productId!!)
                    }
                }

                val action = ProductDetailFragmentDirections
                    .actionProductDetailFragmentToStockInModalFragment(
                        viewModel.product.value!!
                    )
                findNavController().navigate(action)
                true
            }
            R.id.action_add_to_wishlist -> {
                if (userId.isNotBlank()) {
                    viewModel.onAddOrRemoveToWishListClick(
                        viewModel.product.value!!,
                        userId
                    )
                }
                loadingDialog.show()
                true
            }
            else -> false
        }
    }
}
