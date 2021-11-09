package com.teampym.onlineclothingshopapplication.presentation.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private lateinit var binding: FragmentSplashBinding

    private val viewModel: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentSplashBinding.bind(view)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.checkIfUserIsInDb(currentUser.uid)
        } else {
            findNavController().navigate(R.id.action_splashFragment_to_categoryFragment)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.splashEvent.collect { event ->
                when (event) {
                    SplashViewModel.SplashEvent.NotRegistered -> {
                        findNavController().navigate(R.id.action_splashFragment_to_registrationFragment)
                    }
                    SplashViewModel.SplashEvent.Registered -> {
                        findNavController().navigate(R.id.action_splashFragment_to_categoryFragment)
                    }
                }
            }
        }
    }
}
