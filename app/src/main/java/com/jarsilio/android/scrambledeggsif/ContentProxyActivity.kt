package com.jarsilio.android.scrambledeggsif

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jarsilio.android.common.extensions.isJellyBeanOrNewer
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

    private val openGalleryIntent: Intent by lazy {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // I explicitly don't allow to select multiple images because I still haven't been able to deliver the results correctly in a result intent
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        Intent.createChooser(intent, getString(R.string.share_multiple_via))
    }

    private fun scrambleAndFinish(imageUri: Uri) {
        if (utils.isScrambleableImage(imageUri)) {
            Timber.d("Received image to scramble: $imageUri. Scrambling...")
            val scrambledImageUri = ExifScrambler(this).scrambleImage(imageUri)

            // Send data back to calling app
            val resultIntent = Intent()
            resultIntent.data = scrambledImageUri
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file

            Timber.d("Returning uri in result intent: $scrambledImageUri")
            setResult(RESULT_OK, resultIntent)
        } else {
            Timber.d("Received something that's not a jpeg or a png image ($imageUri) in a SEND_MULTIPLE. Skipping...")
            Toast.makeText(this, getString(R.string.image_not_scrambleable, utils.getRealFilenameFromURI(imageUri)), Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
        }

        finish()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun scrambleAndFinish(selectedClipData: ClipData) {
        Timber.d("Received several images to scramble in ClipData: $selectedClipData. Scrambling...")

        val selectedImageUris = ArrayList<Uri>()
        for (i in 0 until selectedClipData.itemCount) {
            selectedImageUris.add(selectedClipData.getItemAt(i).uri)
        }
        val scrambledImageUris = ExifScrambler(this).scrambleImages(selectedImageUris)

        val resultClipData = ClipData("Attachment", arrayOf("image/*"), ClipData.Item(scrambledImageUris.removeAt(0)))
        for (scrambledImageUri in scrambledImageUris) {
            resultClipData.addItem(ClipData.Item(scrambledImageUri))
        }

        Timber.d("Returning uris in ClipData in result intent: $scrambledImageUris")
        val resultIntent = Intent()
        resultIntent.clipData = resultClipData
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file

        setResult(RESULT_OK, resultIntent)

        finish()
    }

    private val openGalleryActivityResultLauncher by lazy { // This can only run during onAttach or onCreate (which is the case because we first use the variable onCreate)
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!
                if (data.data != null) { // Single uri
                    scrambleAndFinish(data.data!!)
                } else if (isJellyBeanOrNewer && data.clipData != null) { // Probably multiple uris. Heavily inspired from https://github.com/SimpleMobileTools/Simple-File-Manager/blob/3284d53d53b545b9beb3351ae9fe9899e2c55b9e/app/src/main/kotlin/com/simplemobiletools/filemanager/pro/activities/MainActivity.kt#L372 (Thank you very much for that @tibbi)
                    scrambleAndFinish(data.clipData!!)
                }
            } else {
                Timber.e("Something went wrong and couldn't scramble any images after returning from the gallery...")
                setResult(RESULT_CANCELED)
                finish()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_content_proxy)

        openGallery()
    }

    private fun openGallery() {
        Timber.d("Opening gallery so that the user can choose some images to share")
        openGalleryActivityResultLauncher.launch(openGalleryIntent)
    }
}
