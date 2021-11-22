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
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val RC_SIGN_IN = 699

private const val TAG = "ProfileFragment"

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)

        val currentUser = getFirebaseUser()

        currentUser?.let { viewModel.checkIfUserIsRegistered(it) }
        viewModel.isRegistered.observe(viewLifecycleOwner) {
            if (!it) {
                findNavController().navigate(R.id.action_profileFragment_to_registrationFragment)
            }
        }

        binding.cardViewBanner.isVisible = currentUser != null && !currentUser.isEmailVerified

        binding.apply {

            tvAddress.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_deliveryInformationFragment)
            }

            tvProfile.setOnClickListener {
                Toast.makeText(requireContext(), tvProfile.text, Toast.LENGTH_SHORT).show()
            }

            tvWishList.setOnClickListener {
                Toast.makeText(requireContext(), tvWishList.text, Toast.LENGTH_SHORT).show()
            }

            btnSendVerification.setOnClickListener {
                FirebaseAuth.getInstance().currentUser?.let { user ->
                    viewModel.sendVerificationAgain(
                        user
                    )
                }
            }

            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser == null) {
                    binding.cardViewBanner.isVisible = false

                    btnSignInAndOut.text = getString(R.string.btn_sign_in)
                    btnSignInAndOut.setOnClickListener {
                        showSignInMethods()
                    }
                } else {
                    btnSignInAndOut.text = getString(R.string.btn_sign_out)
                    btnSignInAndOut.setOnClickListener {
                        showSignOutDialog(auth)
                    }
                }
            }
        }

        viewModel.verificationSpan.observe(viewLifecycleOwner) {
            binding.btnSendVerification.isVisible = it > 0 && System.currentTimeMillis() > it
        }

        viewModel.user.observe(viewLifecycleOwner) { userInformation ->
            userInformation?.let { user ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { taskToken ->
                    if (!taskToken.isSuccessful) {
                        Log.w(TAG, "Fetching FCM registration token failed", taskToken.exception)
                        return@addOnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = taskToken.result.toString()
                    viewModel.onNotificationTokenInserted(user.userId, user.userType, token)

                    // Subscribe to news or else navigate to admin view
                    if (user.userType == UserType.CUSTOMER.name) {
                        Firebase.messaging.subscribeToTopic("news")
                            .addOnCompleteListener { task ->
                                var msg = getString(R.string.msg_subscribed)
                                if (!task.isSuccessful) {
                                    msg = getString(R.string.msg_subscribe_failed)
                                }
                                Log.d(TAG, msg)
                            }.addOnFailureListener {
                                return@addOnFailureListener
                            }
                    } else {
                        // TODO("Navigate to admin view")
                    }

                    Log.d(TAG, token)
                }.addOnFailureListener {
                    return@addOnFailureListener
                }

                Glide.with(requireView())
                    .load(user.avatarUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.ic_user)
                    .into(binding.imgAvatar)

                val fullName = "${user.firstName} ${user.lastName}"
                binding.tvUsername.text = fullName
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.profileEvent.collectLatest { event ->
                when (event) {
                    is ProfileViewModel.ProfileVerificationEvent.NotVerified -> {
                        binding.cardViewBanner.isVisible = true
                    }
                    is ProfileViewModel.ProfileVerificationEvent.VerificationSent -> {
                        Snackbar.make(
                            requireView(),
                            "Verification sent to your email. Please check it.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        viewModel.verificationSpan.value = System.currentTimeMillis() + 3600
                    }
                    is ProfileViewModel.ProfileVerificationEvent.Verified -> {
                        binding.cardViewBanner.isVisible = false
                    }
                }
            }

            while (true) {
                getFirebaseUser()?.let {
                    it.reload()
                    binding.cardViewBanner.isVisible = !it.isEmailVerified
                    delay(5000)
                }
            }
        }
    }

    private fun getFirebaseUser() = FirebaseAuth.getInstance().currentUser

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
            tvUsername.text = resources.getString(R.string.label_guest)
            imgAvatar.setImageResource(R.drawable.ic_user)

            btnSignInAndOut.text = getString(R.string.btn_sign_in)
            cardViewBanner.isVisible = false
        }
    }

    private fun showSignInMethods() {

        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
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
            val user = getFirebaseUser()
            if (user != null) {
                // Try to send email verification for the first time the user uses the app.
                if (!user.isEmailVerified)
                    user.sendEmailVerification()

                viewModel.checkIfUserIsEmailVerified(user)
            }
        }
    }
}
