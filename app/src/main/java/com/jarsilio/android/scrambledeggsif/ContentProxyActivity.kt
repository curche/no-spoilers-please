package com.jarsilio.android.scrambledeggsif

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jarsilio.android.scrambledeggsif.utils.ExifScrambler
import com.jarsilio.android.scrambledeggsif.utils.Utils
import timber.log.Timber

const val PICK_IMAGE_REQUEST = 100

/**
 * Scrambles user-selected file from documentsui.
 *
 * Responds to GET_CONTENT and therefore shows Scrambled Exif
 * in the in ellipsis menu of documentsui.
 */

@ExperimentalUnsignedTypes
class ContentProxyActivity : AppCompatActivity() {

    private val utils: Utils by lazy { Utils(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_proxy)
        openGallery()
    }

    private fun openGallery() {
        Timber.d("Opening gallery so that the user can choose some images to share")

        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // I explicitly don't allow to select multiple images because I still haven't been able to deliver the results correctly in a result intent
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
        }

        startActivityForResult(Intent.createChooser(intent, getString(R.string.share_multiple_via)), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data!! // I checked for null in the if condition
            if (utils.isScrambleableImage(imageUri)) {
                Timber.d("Received image to scramble: $imageUri. Scrambling...")
                val scrambledImageUri = ExifScrambler(this).scrambleImage(imageUri)
                Timber.d("Sending uri in result intent: $scrambledImageUri")

                // Send data back to calling app
                val resultIntent = Intent()
                resultIntent.data = scrambledImageUri
                resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file

                setResult(RESULT_OK, resultIntent)
            } else {
                Timber.d("Received something that's not a jpeg or a png image ($imageUri) in a SEND_MULTIPLE. Skipping...")
                Toast.makeText(this, getString(R.string.image_not_scrambleable, utils.getRealFilenameFromURI(imageUri)), Toast.LENGTH_SHORT).show()
                setResult(RESULT_CANCELED)
            }
        } else {
            setResult(RESULT_CANCELED)
        }

        finish()
    }
}
