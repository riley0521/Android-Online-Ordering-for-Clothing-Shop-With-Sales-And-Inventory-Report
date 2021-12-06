package com.teampym.onlineclothingshopapplication.presentation.client.size

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentSizeChartBinding

class SizeChartFragment : Fragment(R.layout.fragment_size_chart) {

    private lateinit var binding: FragmentSizeChartBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSizeChartBinding.bind(view)

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/uncrowned.appspot.com/o/public%2Fsize-chart-1.jpg?alt=media&token=9186a2a9-19c8-48d7-93fe-4d0c1cf51f30")
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_user)
            .into(binding.imgChart1)

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/uncrowned.appspot.com/o/public%2Fsize-chart-2.jpg?alt=media&token=faffddcc-e2bf-477e-87ca-36baf912343f")
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_user)
            .into(binding.imgChart2)

    }
}
