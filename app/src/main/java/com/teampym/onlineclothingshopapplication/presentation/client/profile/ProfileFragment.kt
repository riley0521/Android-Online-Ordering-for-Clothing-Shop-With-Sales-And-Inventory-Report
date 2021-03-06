package com.teampym.onlineclothingshopapplication.presentation.client.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.data.util.LoadingDialog
import com.teampym.onlineclothingshopapplication.data.util.UserType
import com.teampym.onlineclothingshopapplication.databinding.FragmentProfileBinding
import com.teampym.onlineclothingshopapplication.presentation.registration.ADD_EDIT_PROFILE_REQUEST
import com.teampym.onlineclothingshopapplication.presentation.registration.ADD_EDIT_PROFILE_RESULT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RC_SIGN_IN = 699

private const val TAG = "ProfileFragment"

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding

    private val args by navArgs<ProfileFragmentArgs>()

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var loadingDialog: LoadingDialog

    private var userId = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentProfileBinding.bind(view)
        loadingDialog = LoadingDialog(requireActivity())

        if (args.isSigningIn) {
            showSignInMethods()
        }

        if (args.isBanned) {
            FirebaseAuth.getInstance().currentUser?.let {
                Toast.makeText(
                    requireContext(),
                    "This account is banned for suspicious activities.",
                    Toast.LENGTH_SHORT
                ).show()

                signOut(FirebaseAuth.getInstance())
            }
        }

        setupViews()

        lifecycleScope.launchWhenStarted {
            launch {
                getUserSession()
            }

            launch {
                viewModel.profileEvent.collectLatest { event ->
                    when (event) {
                        is ProfileViewModel.ProfileEvent.VerificationSent -> {
                            loadingDialog.dismiss()

                            Snackbar.make(
                                requireView(),
                                "Verification sent to your email. Please check it.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        ProfileViewModel.ProfileEvent.NotRegistered -> {
                            loadingDialog.dismiss()

                            setFragmentResultListener(ADD_EDIT_PROFILE_REQUEST) { _, bundle ->
                                val result = bundle.getInt(ADD_EDIT_PROFILE_RESULT)
                                viewModel.onAddEditProfileResult(result)
                            }

                            val action =
                                ProfileFragmentDirections.actionProfileFragmentToRegistrationFragment(
                                    false
                                )
                            findNavController().navigate(action)
                        }
                        ProfileViewModel.ProfileEvent.SignedOut -> {
                            loadingDialog.dismiss()

                            resetInformation()
                        }
                        is ProfileViewModel.ProfileEvent.ShowSuccessMessage -> {
                            loadingDialog.dismiss()

                            Snackbar.make(
                                requireView(),
                                event.msg,
                                Snackbar.LENGTH_LONG
                            ).show()

                            getFirebaseUser()?.let {
                                // Re-fetch the userInformation from local db
                                // Then update the View Automatically because of observers
                                loadingDialog.show()
                                viewModel.fetchUserFromLocalDb(it.uid, false)
                            }
                        }
                        is ProfileViewModel.ProfileEvent.ShowErrorMessage -> {
                            loadingDialog.dismiss()

                            Snackbar.make(
                                requireView(),
                                event.msg,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        is ProfileViewModel.ProfileEvent.SignedIn -> {
                            loadingDialog.dismiss()
                            binding.refreshLayout.isRefreshing = false

                            if (event.userInfo != null) {
                                // Check if the user is already registered in the remote db
                                if (event.userInfo.firstName.isBlank()) {
                                    viewModel.navigateUserToRegistrationModule()
                                }

                                binding.apply {

                                    if (event.userInfo.userType == UserType.CUSTOMER.name) {
                                        tvOrders.isVisible = true
                                        tvAddress.isVisible = true
                                        tvProfile.isVisible = true
                                        tvWishList.isVisible = true

                                        // check if the user is already verified in email
                                        val currentUser = getFirebaseUser()
                                        if (currentUser != null) {
                                            cardViewBanner.isVisible = !currentUser.isEmailVerified
                                        }
                                    } else if (event.userInfo.userType == UserType.ADMIN.name) {
                                        tvShippingFees.isVisible = true
                                        tvAccounts.isVisible = true
                                        tvOrders.isVisible = true
                                        tvHistoryLog.isVisible = true
                                        tvSales.isVisible = true
                                        tvSalesAndInventoryReport.isVisible = true
                                        tvSizeChart.isVisible = true
                                    }

                                    // Instantiate view
                                    Glide.with(requireView())
                                        .load(event.userInfo.avatarUrl)
                                        .circleCrop()
                                        .transition(DrawableTransitionOptions.withCrossFade())
                                        .error(R.drawable.ic_user)
                                        .into(imgAvatar)

                                    val fullName =
                                        "${event.userInfo.firstName} ${event.userInfo.lastName}"
                                    tvUsername.text = fullName
                                }
                            }

                            if (event.isFirstTime) {
                                requireActivity().finish()
                                requireActivity().startActivity(requireActivity().intent)
                            }
                        }
                        ProfileViewModel.ProfileEvent.BannedUser -> {
                            loadingDialog.dismiss()

                            Toast.makeText(
                                requireContext(),
                                "This account is banned for suspicious activities.",
                                Toast.LENGTH_SHORT
                            ).show()
                            FirebaseAuth.getInstance().currentUser?.let {
                                signOut(FirebaseAuth.getInstance())
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getUserSession() {
        val pref = viewModel.userSession.first()
        withContext(Dispatchers.Main) {
            Log.d(TAG, "getUserSession: I'm called ${pref.userId}")
        }

        if (pref.userId.isNotBlank()) {
            userId = pref.userId
            viewModel.fetchUserFromLocalDb(pref.userId, false)
        }
    }

    private fun setupViews() = CoroutineScope(Dispatchers.Main).launch {
        binding.apply {

            tvOrders.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_orderFragment)
            }

            tvAddress.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_deliveryInformationFragment)
            }

            tvProfile.setOnClickListener {
                val action =
                    ProfileFragmentDirections.actionProfileFragmentToRegistrationFragment(editMode = true)
                findNavController().navigate(action)
            }

            tvWishList.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_wishListFragment)
            }

            tvShippingFees.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_shippingFeesFragment)
            }

            tvAccounts.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_accountsFragment)
            }

            tvHistoryLog.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_historyLogFragment)
            }

            tvSales.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_salesFragment)
            }

            tvSalesAndInventoryReport.setOnClickListener {
                val reportsIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://docs.google.com/spreadsheets/d/1qu4JbTsNmMXfNLMZFfrROCIUqRKORWKw6Twn2LI9LHc/edit?usp=sharing")
                )
                startActivity(reportsIntent)
            }

            tvToc.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_termsAndConditionFragment)
            }

            tvSizeChart.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_sizeChartFragment)
            }

            tvFaqs.setOnClickListener {
                findNavController().navigate(R.id.action_profileFragment_to_frequentlyAskQuestionsFragment)
            }

            btnSendVerification.setOnClickListener {
                getFirebaseUser()?.sendEmailVerification()
                viewModel.sendVerificationAgain()
            }

            tvGmail.setOnClickListener {
                val gmailIntent = Intent(Intent.ACTION_SEND)
                gmailIntent.data = Uri.parse("mailto:")
                gmailIntent.type = "text/plain"
                gmailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("12mn.clo@gmail.com"))
                gmailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                gmailIntent.putExtra(Intent.EXTRA_TEXT, "")
                startActivity(Intent.createChooser(gmailIntent, "Choose email"))
            }

            refreshLayout.setOnRefreshListener {
                CoroutineScope(Dispatchers.IO).launch {
                    getUserSession()
                }
            }

            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser == null) {
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
    }

    private fun resetInformation() {
        binding.apply {
            tvUsername.text = resources.getString(R.string.label_guest)
            imgAvatar.setImageResource(R.drawable.ic_user)

            btnSignInAndOut.text = getString(R.string.btn_sign_in)
            cardViewBanner.isVisible = false

            tvOrders.isVisible = false
            tvAddress.isVisible = false
            tvProfile.isVisible = false
            tvWishList.isVisible = false

            tvShippingFees.isVisible = false
            tvAccounts.isVisible = false
            tvHistoryLog.isVisible = false
            tvSales.isVisible = false
            tvSalesAndInventoryReport.isVisible = false
            tvSizeChart.isVisible = false

            requireActivity().finish()
            requireActivity().startActivity(requireActivity().intent)
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
                .setTheme(R.style.ThemeOverlay_MaterialComponents_Dark)
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

                loadingDialog.show()
                viewModel.fetchUserFromRemoteDb(user)
            }
        }
    }
}
