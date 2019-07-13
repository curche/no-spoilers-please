/*
 * Copyright (c) 2018-2019 Juan García Basilio
 *
 * This file is part of Scrambled Exif.
 *
 * Scrambled Exif is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrambled Exif is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrambled Exif.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jarsilio.android.scrambledeggsif

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

import java.util.ArrayList

import timber.log.Timber
import android.os.Parcelable

class HandleImageActivity : AppCompatActivity() {

    private val exifScrambler: ExifScrambler by lazy { ExifScrambler(applicationContext) }
    private val utils: Utils by lazy { Utils(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handle_image)

        if (utils.isPermissionGranted) {
            scrambleAndShareImages()
        } else {
            Timber.d("READ_EXTERNAL_STORAGE has not been granted. Showing toast to tell the user to open the app")
            Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show()
        }
        scheduleCacheCleanup()
        finish()
    }

    private fun scrambleAndShareImages() {
        val receivedImages = if (intent.action == Intent.ACTION_SEND) {
            arrayListOf(intent.getParcelableExtra(Intent.EXTRA_STREAM))
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        } else {
            ArrayList<Uri>()
        }

        val scrambledImages = scrambleImages(receivedImages)
        shareImages(scrambledImages)
    }

    private fun scrambleImages(imageUris: ArrayList<Uri>): ArrayList<Uri> {
        Timber.d("Scrambling images")

        val scrambledImages = ArrayList<Uri>()

        for (imageUri in imageUris) {
            if (utils.isImage(imageUri)) {
                Timber.d("Received image (uri): %s. Scrambling...", imageUri)
                scrambledImages.add(exifScrambler.scrambleImage(imageUri))
            } else {
                Timber.d("Received something that's not an image (%s) in a SEND_MULTIPLE. Skipping...", imageUri)
            }
        }

        return scrambledImages
    }

    private fun shareImages(imageUris: ArrayList<Uri>) {
        val targetedShareIntents = buildTargetedShareIntents(imageUris)
        val chooserIntent = Intent.createChooser(targetedShareIntents.removeAt(0), getString(R.string.share_multiple_via))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toTypedArray<Parcelable>())
        startActivity(chooserIntent)
    }

    private fun buildTargetedShareIntents(imageUris: ArrayList<Uri>): ArrayList<Intent> {
        /* Remove our own package from the apps to share with */

        val targetedShareIntents = ArrayList<Intent>()
        val resolveInfos = packageManager.queryIntentActivities(createShareIntent(imageUris), 0)
        for (info in resolveInfos) {
            if (info.activityInfo.packageName.toLowerCase() == packageName.toLowerCase()) {
                continue // Don't add out own package
            }

            val targetedShareIntent = createShareIntent(imageUris)
            targetedShareIntent.setPackage(info.activityInfo.packageName)
            targetedShareIntents.add(targetedShareIntent)
        }

        return targetedShareIntents
    }

    private fun createShareIntent(uris: ArrayList<Uri>): Intent {
        return Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putExtra(Intent.EXTRA_STREAM, uris)
            }
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        }
    }

    private fun scheduleCacheCleanup() {
        Timber.d("Scheduling alarm to clean up cache directory (ExifScramblerCleanUp)")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, CleanUpAlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                AlarmManager.INTERVAL_HALF_DAY,
                alarmPendingIntent)
    }
}