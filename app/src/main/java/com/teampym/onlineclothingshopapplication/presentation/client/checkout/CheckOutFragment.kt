package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentCheckOutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckOutFragment : Fragment(R.layout.fragment_check_out) {

    private lateinit var binding: FragmentCheckOutBinding

    private val viewModel by viewModels<CheckOutViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)

        val user = getFirebaseUser()

        binding.apply {
            btnPlaceOrder.setOnClickListener {
                // TODO("Complete placing order process.")
            }
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser
}