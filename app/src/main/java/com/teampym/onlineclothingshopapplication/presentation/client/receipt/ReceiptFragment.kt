package com.teampym.onlineclothingshopapplication.presentation.client.receipt

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.teampym.onlineclothingshopapplication.R
import com.teampym.onlineclothingshopapplication.databinding.FragmentReceiptBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ReceiptFragment"

class ReceiptFragment : Fragment(R.layout.fragment_receipt) {

    private lateinit var binding: FragmentReceiptBinding

    private val args by navArgs<ReceiptFragmentArgs>()

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentReceiptBinding.bind(view)

        val order = args.order

        binding.apply {
            tvOrderID.text = order.id

            val calendarDate = Calendar.getInstance()
            calendarDate.timeInMillis = order.dateOrdered
            calendarDate.timeZone = TimeZone.getTimeZone("GMT+8:00")
            val formattedDate =
                SimpleDateFormat("MMMM dd yyyy").format(calendarDate.time)

            tvOrderDate.text = formattedDate

            val items = StringBuilder()
            for (i in order.orderDetailList.indices) {
                val item = order.orderDetailList[i]
                val productName = item.product.name
                val productSize = "(${item.size})"
                val quantityAndTotal =
                    "x${item.quantity} = " + getString(R.string.placeholder_price, item.subTotal)

                if (order.orderDetailList.lastIndex == i) {
                    items.append("$productName $productSize $quantityAndTotal")
                } else {
                    items.append("$productName $productSize $quantityAndTotal\n\n")
                }
            }

            tvItems.text = items.toString()

            tvSubTotal.text = getString(R.string.placeholder_price, order.totalCost)
            tvShippingFee.text = getString(R.string.placeholder_price, order.shippingFee)
            tvGrandTotal.text =
                getString(R.string.placeholder_price, order.totalPaymentWithShippingFee)
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.receipt_action_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                val bitmap = getScreenShotFromView(binding.mainLayout)
                bitmap?.let {
                    saveMediaToStorage(bitmap)
                }
                true
            }
            else -> false
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        val fileName = "Midnightmares - ${args.order.id}.jpg"

        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireActivity().contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imageDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imageDir, fileName)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(
                requireContext(),
                "Saved Image To Gallery.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getScreenShotFromView(mainLayout: View): Bitmap? {
        var screenShot: Bitmap? = null
        try {
            screenShot = Bitmap.createBitmap(
                mainLayout.measuredWidth,
                mainLayout.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(screenShot)
            mainLayout.draw(canvas)
        } catch (ex: Exception) {
            Log.d(TAG, "getScreenShotFromView: ${ex.message}")
        }
        return screenShot
    }
}
