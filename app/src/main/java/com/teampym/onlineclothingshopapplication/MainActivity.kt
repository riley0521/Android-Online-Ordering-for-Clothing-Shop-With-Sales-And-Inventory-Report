package com.teampym.onlineclothingshopapplication

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.teampym.onlineclothingshopapplication.data.util.CHANNEL_ID
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set splash screen here
        setContentView(R.layout.activity_main)

        // Create notification channel when opening your app
        // It's okay since you only have SAA (Single Activity Application)
        createNotificationChannel()

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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navController.handleDeepLink(intent)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.simple_desc_of_app)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

const val VERIFICATION_SPAN = "verification_span"

const val ADD_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER
const val EDIT_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 1
const val DELETE_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 2

const val ADD_DELIVERY_INFO_RESULT_ERR = -1
const val EDIT_DELIVERY_INFO_RESULT_ERR = -2
const val DELETE_DELIVERY_INFO_RESULT_ERR = -3
