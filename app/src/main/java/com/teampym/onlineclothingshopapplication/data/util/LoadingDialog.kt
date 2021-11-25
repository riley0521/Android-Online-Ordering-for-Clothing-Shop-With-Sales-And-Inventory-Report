package com.teampym.onlineclothingshopapplication.data.util

import android.app.Activity
import android.app.AlertDialog
import com.teampym.onlineclothingshopapplication.R

class LoadingDialog(act: Activity) {
    private var dialog: AlertDialog

    init {
        val viewInstance = act.layoutInflater.inflate(R.layout.circular_loading_bar, null)

        dialog = AlertDialog.Builder(act)
            .setView(viewInstance)
            .setCancelable(false)
            .create()
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun isActive() = dialog.isShowing
}
