package com.teampym.onlineclothingshopapplication.presentation.client.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
import com.teampym.onlineclothingshopapplication.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RC_SIGN_IN = 699

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var userInformationDao: UserInformationDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            viewModel.getCurrentUser(currentUser.uid)
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

            FirebaseAuth.getInstance().addAuthStateListener { auth ->

                if (currentUser != null) {
                    binding.cardViewBanner.isVisible = currentUser.isEmailVerified

                    btnSignOut.text = "Sign Out"
                    btnSignOut.setOnClickListener {
                        showSignOutDialog(auth)
                    }
                } else {
                    viewModel.verificationSpan.value = 0
                    btnSignOut.text = "Sign In"
                    btnSignOut.setOnClickListener {
                        showSignInMethods()
                    }
                }
            }
        }

        viewModel.verificationSpan.observe(viewLifecycleOwner) {
            binding.btnSendVerification.isVisible = it > 0 && System.currentTimeMillis() > it
        }

        lifecycleScope.launch {
            viewModel.userInformation.observe(viewLifecycleOwner) { userInfo ->
                if(userInfo.avatarUrl.isNotEmpty())
                    Glide.with(requireView())
                        .load(userInfo.avatarUrl)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .error(R.drawable.ic_user)
                        .into(binding.imgAvatar)

                val fullName = "${userInfo.firstName} ${userInfo.lastName}"
                binding.tvUsername.text = fullName
            }

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
                    ProfileViewModel.ProfileEvent.Verified -> {
                        binding.cardViewBanner.isVisible = false
                    }
                }
            }

            while (true) {
                currentUser?.let {
                    it.reload()
                    binding.cardViewBanner.isVisible = !it.isEmailVerified
                    delay(3000)
                }
            }
        }
    }

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
            if(user != null) {
                viewModel.checkIfUserIsRegisteredOrVerified(user)
            }
        }
    }

}