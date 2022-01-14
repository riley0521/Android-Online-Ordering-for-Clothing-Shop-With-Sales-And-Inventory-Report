package com.teampym.onlineclothingshopapplication.presentation.client.request_return_item

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.OrderDetail
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.SELECT_MULTIPLE_ADDITIONAL_IMAGES
import com.teampym.onlineclothingshopapplication.databinding.FragmentRequestReturnItemBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST = 1111

@AndroidEntryPoint
class RequestReturnItemFragment :
    Fragment(R.layout.fragment_request_return_item),
    RequestReturnItemAdapter.RequestReturnListener {

    private lateinit var binding: FragmentRequestReturnItemBinding

    private lateinit var adapter: RequestReturnItemAdapter

    private lateinit var loadingDialog: LoadingDialog

    private val viewModel by viewModels<RequestReturnItemViewModel>()

    private val args by navArgs<RequestReturnItemFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRequestReturnItemBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        val orderItem: OrderDetail? = args.orderItem

        if (args.isViewing && orderItem == null) {
            binding.btnAddPhotos.isVisible = false
            binding.btnSubmit.isVisible = false
            binding.etReturnReason.isEnabled = false

            viewModel.fetchReturnItem(args.orderItemId)
        }

        if (orderItem != null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                "Product ${orderItem.product.name}"
        }

        viewModel.returnItem.observe(viewLifecycleOwner) {
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                "Product ${it.productDetail}"

            binding.etReturnReason.setText(it.reason)
        }

        viewModel.imageListUri.observe(viewLifecycleOwner) {
            adapter = RequestReturnItemAdapter(this, it)

            binding.apply {
                rvPhotos.setHasFixedSize(true)
                rvPhotos.layoutManager = LinearLayoutManager(requireContext())
                rvPhotos.adapter = adapter
            }
        }

        viewModel.uploadImages.observe(viewLifecycleOwner) {
            adapter = RequestReturnItemAdapter(this, it)

            binding.apply {
                rvPhotos.setHasFixedSize(true)
                rvPhotos.layoutManager = LinearLayoutManager(requireContext())
                rvPhotos.adapter = adapter
            }
        }

        binding.apply {
            etReturnReason.setText(viewModel.reason)

            etReturnReason.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Nothing
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.reason = s.toString()
                }

                override fun afterTextChanged(s: Editable?) {
                    // Nothing
                }
            })

            btnAddPhotos.setOnClickListener {
                Intent(Intent.ACTION_GET_CONTENT).also {
                    it.type = "image/*"
                    it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    startActivityForResult(
                        Intent.createChooser(
                            it,
                            SELECT_MULTIPLE_ADDITIONAL_IMAGES
                        ),
                        SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST
                    )
                }
            }

            btnSubmit.setOnClickListener {
                loadingDialog.show()
                viewModel.onSubmitClicked(orderItem!!)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.requestReturnEvent.collectLatest { event ->
                when (event) {
                    is RequestReturnItemViewModel.RequestReturnEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is RequestReturnItemViewModel.RequestReturnEvent.ShowSuccessMessage -> {
                        loadingDialog.dismiss()

                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()

                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_MULTIPLE_IMAGE_FROM_GALLERY_REQUEST) {
            data?.clipData?.let {

                // Loop all selected images then add it to the list.
                // Then you can save its state just in case of process death.
                val imageUriList = mutableListOf<Uri>()
                for (pos in 0 until it.itemCount) {
                    imageUriList.add(it.getItemAt(pos).uri)
                }
                viewModel.updateImageListUri(imageUriList)
            }
        }
    }

    override fun onRemoveClicked(position: Int) {
        viewModel.removeImage(position)
    }
}
