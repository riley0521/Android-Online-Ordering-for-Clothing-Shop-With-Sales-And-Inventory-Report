package com.teampym.onlineclothingshopapplication.presentation.client.size

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.SizeChart
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentSizeChartBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

private const val SELECT_SIZE_CHART_IMG_REQUEST = 2240

@AndroidEntryPoint
class SizeChartFragment :
    Fragment(R.layout.fragment_size_chart),
    SizeChartImageAdapter.SizeChartImageListener {

    private lateinit var binding: FragmentSizeChartBinding

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var adapter: SizeChartImageAdapter

    private val viewModel by viewModels<SizeChartViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSizeChartBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        instantiateAdapter(false)

        viewModel.fetchSizeChartImages()

        viewModel.sizeChartImages.observe(viewLifecycleOwner) {
            adapter.submitList(it)

            binding.apply {
                rvSizeChartImages.setHasFixedSize(true)
                rvSizeChartImages.layoutManager = LinearLayoutManager(requireContext())
                rvSizeChartImages.adapter = adapter
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.sizeChartEvent.collectLatest { event ->
                when(event) {
                    is SizeChartViewModel.SizeChartEvent.ShowErrorMessage -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is SizeChartViewModel.SizeChartEvent.ShowMessageAndAddItemToAdapter -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        viewModel.fetchSizeChartImages()
                    }
                    is SizeChartViewModel.SizeChartEvent.ShowMessageAndNotifyAdapter -> {
                        loadingDialog.dismiss()
                        Snackbar.make(
                            requireView(),
                            event.msg,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        adapter.notifyItemRemoved(event.position)
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    private fun instantiateAdapter(adminMode: Boolean) {
        adapter = SizeChartImageAdapter(this, adminMode)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.size_chart_action_menu, menu)

        viewModel.userSession.observe(viewLifecycleOwner) {
            if (it.userType == UserType.ADMIN.name) {
                menu.findItem(R.id.action_add_image).isVisible = true
                menu.findItem(R.id.action_delete_images).isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_image -> {
                Intent(Intent.ACTION_GET_CONTENT).also {
                    it.type = "image/*"
                    startActivityForResult(it, SELECT_SIZE_CHART_IMG_REQUEST)
                }
                true
            }
            R.id.action_delete_images -> {
                instantiateAdapter(true)
                adapter.submitList(viewModel.sizeChartImages.value)
                true
            }
            else -> false
        }
    }

    override fun onRemoveClicked(sizeChart: SizeChart, position: Int) {
        viewModel.removeSizeChart(sizeChart, position)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == SELECT_SIZE_CHART_IMG_REQUEST) {
            data?.data?.let {
                loadingDialog.show()
                viewModel.uploadSizeChart(it)
            }
        }
    }
}
