package com.teampym.onlineclothingshopapplication.presentation

import android.app.Application
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment.SANDBOX
import com.paypal.checkout.config.SettingsConfig
import com.paypal.checkout.createorder.CurrencyCode
import com.paypal.checkout.createorder.UserAction
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OnlineClothingShop : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = CheckoutConfig(
            application = this,
            clientId = "AXnccMcdGZRG-mAUqixz5kkFMQIDu8xbBzMzmJoOiz-gol5WyT_PXw1x-1xv5iodwvClmGJKOIjuQO0K",
            environment = SANDBOX,
            returnUrl = "com.teampym.onlineclothingshopapplication://paypalpay",
            currencyCode = CurrencyCode.PHP,
            userAction = UserAction.PAY_NOW,
            settingsConfig = SettingsConfig(loggingEnabled = true)
        )
        PayPalCheckout.setConfig(config)
    }
}
