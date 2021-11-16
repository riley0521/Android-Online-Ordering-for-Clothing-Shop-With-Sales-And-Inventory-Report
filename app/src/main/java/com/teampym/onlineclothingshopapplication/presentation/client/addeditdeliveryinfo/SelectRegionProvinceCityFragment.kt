package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Selector
import com.teampym.onlineclothingshopapplication.databinding.FragmentSelectRegionProvinceCityBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectRegionProvinceCityFragment :
    Fragment(R.layout.fragment_select_region_province_city),
    SelectRegionProvinceCityAdapter.OnSelectorListener {

    private lateinit var binding: FragmentSelectRegionProvinceCityBinding

    private val viewModel: DeliveryInfoSharedViewModel by activityViewModels()

    private val args by navArgs<SelectRegionProvinceCityFragmentArgs>()

    private lateinit var adapter: SelectRegionProvinceCityAdapter

    private var regionId = 0L
    private var provinceId = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSelectRegionProvinceCityBinding.bind(view)

        regionId = args.regionId
        provinceId = args.provinceId

        adapter = SelectRegionProvinceCityAdapter(this)

        if (regionId == 0L) {
            viewModel.regions.observe(viewLifecycleOwner) { regionList ->
                val selectorList = mutableListOf<Selector>()
                regionList.forEach {
                    selectorList.add(
                        Selector(id = it.id, parentId = 0L, name = it.name)
                    )
                }
                adapter.submitList(selectorList)
            }
        } else if (regionId > 0L && provinceId == 0L) {
            viewModel.getAllProvince(regionId).observe(viewLifecycleOwner) { provinceList ->
                val selectorList = mutableListOf<Selector>()
                provinceList.forEach {
                    selectorList.add(
                        Selector(id = it.id, parentId = it.regionId, name = it.name)
                    )
                }
                adapter.submitList(selectorList)
            }
        } else if (regionId > 0L && provinceId > 0L) {
            viewModel.getAllCity(provinceId).observe(viewLifecycleOwner) { cityList ->
                val selectorList = mutableListOf<Selector>()
                cityList.forEach {
                    selectorList.add(
                        Selector(id = it.id, parentId = it.provinceId, name = it.name)
                    )
                }
                adapter.submitList(selectorList)
            }
        }

        binding.apply {
            recyclerSelector.setHasFixedSize(true)
            recyclerSelector.adapter = adapter
        }
    }

    override fun onItemSelected(selector: Selector) {
        if (regionId == 0L) {
            viewModel.onSelectedRegion(selector)
        } else if (regionId > 0L && provinceId == 0L) {
            viewModel.onSelectedProvince(selector)
        } else if (regionId > 0L && provinceId > 0L) {
            viewModel.onSelectedCity(selector)
        }
        findNavController().popBackStack()
    }
}
