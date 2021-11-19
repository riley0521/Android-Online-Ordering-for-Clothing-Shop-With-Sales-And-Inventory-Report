package com.teampym.onlineclothingshopapplication

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set splash screen here
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.GONE

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.categoryFragment,
                R.id.newsFragment,
                R.id.orderFragment,
                R.id.cartFragment,
                R.id.profileFragment
            )
        )

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main_fragment) as NavHostFragment

        navController = navHostFragment.findNavController()

        navController.addOnDestinationChangedListener { controller, _, _ ->
            if (controller.currentDestination?.id == R.id.categoryFragment ||
                controller.currentDestination?.id == R.id.newsFragment ||
                controller.currentDestination?.id == R.id.orderFragment ||
                controller.currentDestination?.id == R.id.cartFragment ||
                controller.currentDestination?.id == R.id.profileFragment
            ) {
                bottomNav.visibility = View.VISIBLE
            } else
                bottomNav.visibility = View.GONE
        }

        // set up app bar with current destination's label.
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}

const val VERIFICATION_SPAN = "verification_span"

const val ADD_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val DELETE_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 2

const val ADD_DELIVERY_INFO_RESULT_ERR = -1
const val EDIT_DELIVERY_INFO_RESULT_ERR = -2
const val DELETE_DELIVERY_INFO_RESULT_ERR = -3