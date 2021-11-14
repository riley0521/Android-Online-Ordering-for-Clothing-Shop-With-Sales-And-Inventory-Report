package com.teampym.onlineclothingshopapplication.presentation.client.addeditdeliveryinfo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.teampym.onlineclothingshopapplication.databinding.FragmentAddEditDeliveryInformationBinding

class AddEditDeliveryInformationFragment : Fragment() {

    private lateinit var binding: FragmentAddEditDeliveryInformationBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddEditDeliveryInformationBinding.bind(view)
    }
}
