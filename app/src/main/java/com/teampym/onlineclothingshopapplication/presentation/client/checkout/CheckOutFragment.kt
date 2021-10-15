package com.teampym.onlineclothingshopapplication.presentation.client.checkout

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentCheckOutBinding

class CheckOutFragment : Fragment(R.layout.fragment_check_out) {
    private lateinit var binding: FragmentCheckOutBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCheckOutBinding.bind(view)

        val user = FirebaseAuth.getInstance().currentUser

        binding.apply {
            btnPlaceOrder.setOnClickListener {
                if(user == null)
                    Toast.makeText(requireContext(), "Please sign in first", Toast.LENGTH_SHORT).show()
                else {
                    // Place order here.
                }
            }
        }
    }
}