package com.teampym.onlineclothingshopapplication.presentation.client.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private const val RC_SIGN_IN = 699

private const val TAG = "ProfileFragment"

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        binding.cardViewBanner.isVisible = getCurrentUser() != null

        binding.apply {

            tvAddress.setOnClickListener {
                Toast.makeText(requireContext(), tvAddress.text, Toast.LENGTH_SHORT).show()
            }

            tvProfile.setOnClickListener {
                Toast.makeText(requireContext(), tvProfile.text, Toast.LENGTH_SHORT).show()
            }

            tvWishList.setOnClickListener {
                Toast.makeText(requireContext(), tvWishList.text, Toast.LENGTH_SHORT).show()
            }

            btnSendVerification.setOnClickListener {
                viewModel.sendVerificationAgain(FirebaseAuth.getInstance().currentUser!!)
            }

            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if(auth.currentUser == null) {
                    binding.cardViewBanner.isVisible = false

                    btnSignOut.text = "Sign In"
                    btnSignOut.setOnClickListener {
                        showSignInMethods()
                    }
                    return@addAuthStateListener
                }

                btnSignOut.text = "Sign Out"
                btnSignOut.setOnClickListener {
                    showSignOutDialog(auth)
                }
            }
        }

        viewModel.verificationSpan.observe(viewLifecycleOwner) {
            binding.btnSendVerification.isVisible = it > 0 && System.currentTimeMillis() > it
        }

        viewModel.user.observe(viewLifecycleOwner) { userInfo ->
            if(userInfo != null) {
                Log.d(TAG, "${userInfo?.firstName}")

                Glide.with(requireView())
                    .load(userInfo.avatarUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_user)
                    .into(binding.imgAvatar)

                val fullName = "${userInfo.firstName} ${userInfo.lastName}"
                binding.tvUsername.text = fullName
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.profileEvent.collect { event ->
                when (event) {
                    is ProfileViewModel.ProfileEvent.NotRegistered -> {
                        findNavController().navigate(R.id.action_profileFragment_to_registrationFragment)
                    }
                    is ProfileViewModel.ProfileEvent.NotVerified -> {
                        binding.cardViewBanner.isVisible = true
                    }
                    ProfileViewModel.ProfileEvent.VerificationSent -> {
                        Snackbar.make(
                            requireView(),
                            "Verification sent to your email. Please check it.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.verificationSpan.value = System.currentTimeMillis() + 3600
                    }
                    ProfileViewModel.ProfileEvent.Verified -> {
                        binding.cardViewBanner.isVisible = false
                    }
                }
            }

            while (true) {
                if(getCurrentUser() != null) {
                    val it = getCurrentUser()
                    it?.reload()
                    binding.cardViewBanner.isVisible = !it?.isEmailVerified!!
                    delay(3000)
                }
            }
        }
    }

    private fun getCurrentUser() = FirebaseAuth.getInstance().currentUser

    private fun showSignOutDialog(auth: FirebaseAuth) {
        AlertDialog.Builder(requireContext())
            .setTitle("SIGN OUT")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton(
                "YES"
            ) { dialog, _ ->
                signOut(auth)
                dialog.dismiss()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun signOut(auth: FirebaseAuth) {
        viewModel.signOut(auth)
        resetInformation()
    }

    private fun resetInformation() {
        binding.apply {
            tvUsername.text = resources.getString(R.string.label_uncrowned_guest)
            imgAvatar.setImageResource(R.drawable.ic_user)

            btnSignOut.text = "SIGN IN"
            cardViewBanner.isVisible = false
        }
    }

    private fun showSignInMethods() {

        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.crown)
                .setTheme(R.style.ThemeOverlay_MaterialComponents_Dark)
                .setTosAndPrivacyPolicyUrls(
                    "https://example.com/terms.html",
                    "https://example.com/privacy.html"
                )
                .build(),
            RC_SIGN_IN
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK) {
            // Successfully signed in
            val user = getCurrentUser()
            if(user != null) {
                // Try to send email verification for the first time the user uses the app.
                if(user.isEmailVerified.not())
                    user.sendEmailVerification()

                viewModel.checkIfUserIsRegisteredOrVerified(user)
            }
        }
    }

}