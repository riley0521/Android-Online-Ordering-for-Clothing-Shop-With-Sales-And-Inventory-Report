package com.teampym.onlineclothingshopapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.visibility = View.GONE
        supportActionBar?.hide()

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.splashFragment,
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

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (controller.currentDestination?.id == R.id.categoryFragment ||
                controller.currentDestination?.id == R.id.newsFragment ||
                controller.currentDestination?.id == R.id.orderFragment ||
                controller.currentDestination?.id == R.id.cartFragment ||
                controller.currentDestination?.id == R.id.profileFragment
            ) {
                bottomNav.visibility = View.VISIBLE
            } else
                bottomNav.visibility = View.GONE

            if (controller.currentDestination?.id != R.id.splashFragment)
                supportActionBar?.show()
        }

        // set up app bar with current destination's label.
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}