package com.teampym.onlineclothingshopapplication

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.util.CHANNEL_ID
import com.teampym.onlineclothingshopapplication.data.util.UserType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_OnlineClothingShop)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // write permission to access the storage
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        CoroutineScope(Dispatchers.IO).launch {
            val userType = PreferencesManager(this@MainActivity).getUserType()

            withContext(Dispatchers.Main) {
                // Create notification channel when opening your app
                // It's okay since you only have SAA (Single Activity Application)
                createNotificationChannel()

                val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
                bottomNav.visibility = View.GONE

                if (userType == UserType.ADMIN.name) {
                    bottomNav.menu.removeItem(R.id.cartFragment)
                }

                appBarConfiguration = AppBarConfiguration(
                    setOf(
                        R.id.categoryFragment,
                        R.id.newsFragment,
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

                supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@MainActivity, R.color.teal_200)))

                listenToDynamicLinks()
            }
        }
    }

    private fun listenToDynamicLinks() {
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener(this) { pendingDynamicLinkData ->

                if (pendingDynamicLinkData != null) {
                    pendingDynamicLinkData.link?.let { uri ->
                        val path = uri.toString().substring(uri.toString().lastIndexOf("/") + 1)

                        // In case if you have multiple shareable items such as User Post, User Profile,
                        // you can check if
                        // the uri contains the required string.
                        // In our case we will check if the path contains the string, 'post'

                        when {
                            uri.toString().contains("post") -> {
                                val postId = path
                                // Call your API or DB to get the post with the ID [postId]
                                // and open the required screen here.
                            }
                            uri.toString().contains("product") -> {
                                // Open the required screen here.
                                navController.navigate(
                                    R.id.productDetailFragment,
                                    bundleOf(
                                        "productId" to path
                                    )
                                )
                            }
                        }
                    }
                }
            }.addOnFailureListener {
                // This lambda will be triggered when there is a failure.
                // Handle
                Log.d(TAG, "handleIncomingDeepLinks: ${it.message}")
            }
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

const val ADD_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 69
const val EDIT_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 70
const val DELETE_DELIVERY_INFO_RESULT_OK = Activity.RESULT_FIRST_USER + 71

const val ADD_DELIVERY_INFO_RESULT_ERR = -69
const val EDIT_DELIVERY_INFO_RESULT_ERR = -70
const val DELETE_DELIVERY_INFO_RESULT_ERR = -71

const val ADD_PROFILE_OK = Activity.RESULT_FIRST_USER + 111
const val EDIT_PROFILE_OK = Activity.RESULT_FIRST_USER + 112

const val ADD_PROFILE_ERR = -111
const val EDIT_PROFILE_ERR = -112
