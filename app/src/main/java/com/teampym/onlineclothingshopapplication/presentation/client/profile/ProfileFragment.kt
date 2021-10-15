package com.teampym.onlineclothingshopapplication.presentation.client.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.models.Utils
import com.teampym.onlineclothingshopapplication.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

private const val RC_SIGN_IN = 699

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val viewModel: ProfileViewModel by viewModels()

    private var currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        val firstName = Utils.currentUser?.firstName ?: ""
        val lastName = Utils.currentUser?.lastName ?: ""

        if(firstName.isNotBlank() && lastName.isNotBlank()) {
            binding.tvName.text = "$firstName $lastName"
        }

        binding.cardViewBanner.isVisible = currentUser != null

        binding.apply {

            tvAddress.setOnClickListener {
                Toast.makeText(requireContext(), tvAddress.text, Toast.LENGTH_SHORT).show()
            }

            tvProfile.setOnClickListener {
                Toast.makeText(requireContext(), tvProfile.text, Toast.LENGTH_SHORT).show()
            }

            tvPassword.setOnClickListener {
                Toast.makeText(requireContext(), tvPassword.text, Toast.LENGTH_SHORT).show()
            }

            btnSignOut.setOnClickListener {
                Toast.makeText(requireContext(), btnSignOut.text, Toast.LENGTH_SHORT).show()
                // Sign out and navigate to log in fragment
            }

            btnSendVerification.setOnClickListener {
                viewModel.sendVerificationAgain(FirebaseAuth.getInstance().currentUser!!)
            }

            FirebaseAuth.getInstance().addAuthStateListener {
                if (it.currentUser == null) {

                    viewModel.verificationSpan.value = 0

                    btnSignOut.text = "Sign In"
                    btnSignOut.setOnClickListener {
                        showSignInMethods()

                    }
                } else {

                    binding.cardViewBanner.isVisible = !it.currentUser!!.isEmailVerified

                    btnSignOut.text = "Sign Out"
                    btnSignOut.setOnClickListener {
                        showSignOutDialog()
                    }
                }
            }
        }

        viewModel.verificationSpan.observe(viewLifecycleOwner) {
            binding.btnSendVerification.isVisible = it > 0 && System.currentTimeMillis() > it
        }

        lifecycleScope.launchWhenStarted {
            viewModel.profileEvent.collect { event ->
                when (event) {
                    is ProfileViewModel.ProfileEvent.NotRegistered -> {
                        findNavController().navigate(R.id.action_profileFragment_to_registrationFragment)
                    }
                    is ProfileViewModel.ProfileEvent.NotVerified -> {
                        viewModel.sendVerificationAgain(FirebaseAuth.getInstance().currentUser!!)
                    }
                    ProfileViewModel.ProfileEvent.VerificationSent -> {
                        Snackbar.make(
                            requireView(),
                            "Verification sent to your email. Please check it.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.verificationSpan.value = System.currentTimeMillis() + 3600
                    }
                }
            }

            while(true) {
                currentUser?.let {
                    it.reload()
                    binding.cardViewBanner.isVisible = !it.isEmailVerified
                    delay(3000)
                }
            }
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("SIGN OUT")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton(
                "YES"
            ) { dialog, _ ->
                signOut()
                dialog.dismiss()
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun signOut() {
        viewModel.signOut(FirebaseAuth.getInstance())
        resetInformation()
        FirebaseAuth.getInstance().signOut()
    }

    private fun resetInformation() {
        binding.apply {
            tvName.text = resources.getString(R.string.label_uncrowned_guest)
            imgAvatar.setImageResource(R.drawable.ic_user)
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
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                viewModel.checkIfUserIsVerified(user)
            }
        }
    }

}